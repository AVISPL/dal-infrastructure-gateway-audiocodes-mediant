/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import org.springframework.web.util.UriComponentsBuilder;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.EndpointStatistics;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.Communicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.DataConversionException;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;
import com.avispl.symphony.dal.util.ControllablePropertyFactory;
import com.avispl.symphony.dal.util.StringUtils;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.NetworkProperty;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallCapacityStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallLoadStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallMediaIssuesStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallQualityStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallRoutingStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallTerminationStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallTrafficStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.Alarms;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.AlarmsResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.KpiValue;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallDialResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallStatus;


/**
 * AudioCodesMediantCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class AudioCodesMediantCommunicator extends Communicator implements Monitorable, Controller {
	private final ExtendedStatistics localExtendedStatistics = new ExtendedStatistics();
	private final EndpointStatistics localEndpointStatistics = new EndpointStatistics();
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/** Adapter metadata properties - adapter version and build date */
	private final Properties adapterProperties = new Properties();
	/** Cached device status; defaults to an empty {@link Status} to avoid {@code null} checks. */
	private Status deviceStatus = new Status();

	/** Device adapter instantiation timestamp. */
	private final long adapterInitializationTimestamp = System.currentTimeMillis();

	/**
	 * Cached list containing all active system alarms and alerts reported by the device.
	 */
	private List<Alarms> alarmsList;

	/** Session id of the currently active (or last dialed) SIP test call; {@code null} if none has been dialed. */
	private String callDiagnosticSessionId;
	/** Staged {@code calledNumber} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#CalledNumber}. */
	private String callDiagnosticCalledNumber = "";
	/** Staged {@code callingNumber} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#CallingNumber}. */
	private String callDiagnosticCallingNumber = "";
	/** Staged {@code destAddress} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#Destination}. */
	private String callDiagnosticDestination = "";
	/** Last known status of the SIP test call (device's {@code callStatus}, or a local sentinel before the first dial/after a manual stop). */
	private String callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_NOT_DIALED;
	/** Device-reported {@code callId} of the currently/last tracked test call. */
	private String callDiagnosticCallId = Constant.NOT_AVAILABLE;
	/** Device-reported {@code releaseCause}; only populated once the call is {@code Disconnected}/{@code Failed}. */
	private String callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;

	/**
	 * Names of the optional statistics groups to pull from the device and display: the KPI-heavy
	 * call-statistics groups (e.g. {@code CallLoadStatistics}, {@code CallRoutingStatistics}) and
	 * {@link Constant#CALL_DIAGNOSTICS_GROUP}. Empty by default, meaning none of them are fetched -
	 * each KPI group is a per-KPI request against the device, and {@code CallDiagnostics} involves its
	 * own test-call session tracking, so groups the caller doesn't ask for are skipped entirely rather
	 * than fetched and hidden. Including {@link Constant#CALL_STATS_ALL_GROUPS} enables every group
	 * regardless of what else is listed.
	 */
	private List<String> displayPropertyGroups = new ArrayList<>();

	/**
	 * Jackson ObjectMapper for JSON deserialization. Initialized eagerly in the constructor
	 * to prevent NullPointerException if {@code convertNode} is invoked before {@code internalInit}.
	 */
	private ObjectMapper objectMapper;


	public AudioCodesMediantCommunicator() throws IOException {
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
	}

	public String getDisplayPropertyGroups() {
		return String.join(",", displayPropertyGroups);
	}

	/**
	 * Sets which optional statistics groups (e.g. {@code CallLoadStatistics}, {@code CallRoutingStatistics},
	 * {@link Constant#CALL_DIAGNOSTICS_GROUP}) to pull from the device and display, from a comma-separated
	 * string. Groups not listed here are skipped entirely on every poll cycle - no request is made and no
	 * stats are emitted for them. Passing {@link Constant#CALL_STATS_ALL_GROUPS} (alone or alongside other
	 * names) enables every group.
	 *
	 * @param displayPropertyGroups comma-separated group names; blank/empty clears the list (nothing displayed)
	 */
	public void setDisplayPropertyGroups(String displayPropertyGroups) {
		if (StringUtils.isNullOrEmpty(displayPropertyGroups, true)) {
			this.displayPropertyGroups = new ArrayList<>();
			return;
		}
		this.displayPropertyGroups = Arrays.stream(displayPropertyGroups.split(","))
				.map(String::strip)
				.filter(group -> !group.isEmpty())
				.collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		this.alarmsList = new ArrayList<>();
		this.objectMapper = new ObjectMapper();
		this.setTrustAllCertificates(true);
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		//  Clear the extended properties
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		//	Clear the populated data
		this.adapterProperties.clear();
		this.deviceStatus = new Status();
		this.callDiagnosticSessionId = null;
		this.callDiagnosticCalledNumber = "";
		this.callDiagnosticCallingNumber = "";
		this.callDiagnosticDestination = "";
		this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_NOT_DIALED;
		this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
		this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			Map<String, String> stats = new HashMap<>();
			List<AdvancedControllableProperty> controls = new ArrayList<>();
			this.setupData();
			retrieveMetadata(stats);
			retrieveDeviceStatus(stats);
			retrieveCallStats(stats);
			retrieveCallDiagnostics(stats, controls);
			stats.putAll(Util.generateActiveAlarmsProperties(this.alarmsList));
			this.localExtendedStatistics.setStatistics(stats);
			this.localExtendedStatistics.setControllableProperties(controls);
		} finally {
			reentrantLock.unlock();
		}
		return List.of(this.localExtendedStatistics, this.localEndpointStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			String property = controllableProperty.getProperty();
			Object value = controllableProperty.getValue();
			String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
			String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
			String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
			String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
			String callIdKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALL_ID;
			String releaseCauseKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_RELEASE_CAUSE;
			String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
			String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

			if (calledNumberKey.equals(property)) {
				this.callDiagnosticCalledNumber = String.valueOf(value);
			} else if (callingNumberKey.equals(property)) {
				this.callDiagnosticCallingNumber = String.valueOf(value);
			} else if (destinationKey.equals(property)) {
				this.callDiagnosticDestination = String.valueOf(value);
			} else if (startKey.equals(property)) {
				startCallDiagnostic();
			} else if (stopKey.equals(property)) {
				stopCallDiagnostic();
			} else {
				throw new IllegalArgumentException("Unsupported control property: '%s'".formatted(property));
			}

			//	Reflect the change in the live stats map immediately rather than waiting for the next poll cycle.
			Map<String, String> liveStats = this.localExtendedStatistics.getStatistics();
			liveStats.put(calledNumberKey, this.callDiagnosticCalledNumber);
			liveStats.put(callingNumberKey, this.callDiagnosticCallingNumber);
			liveStats.put(destinationKey, this.callDiagnosticDestination);
			liveStats.put(statusKey, this.callDiagnosticStatus);
			liveStats.put(callIdKey, this.callDiagnosticCallId);
			liveStats.put(releaseCauseKey, this.callDiagnosticReleaseCause);

			List<AdvancedControllableProperty> liveControls = this.localExtendedStatistics.getControllableProperties();
			liveControls.removeIf(control -> stopKey.equals(control.getName()));
			if (isCallDiagnosticStoppable()) {
				liveStats.put(stopKey, "");
				liveControls.add(ControllablePropertyFactory.createButton(stopKey, "Stop", "Stopping...", 0L));
			} else {
				liveStats.remove(stopKey);
			}
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (controllableProperties == null) {
			return;
		}
		for (ControllableProperty controllableProperty : controllableProperties) {
			controlProperty(controllableProperty);
		}
	}

	/**
	 * Retrieves metadata information and updates the provided statistics and dynamic map.
	 *
	 * @param stats the map where statistics will be stored
	 */
	private void retrieveMetadata(Map<String, String> stats) {
		try {
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_VERSION,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.version")));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_BUILD_DATE,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.build.date")));

			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME, Util.formatUpTime(adapterUptime / 1000));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));

			String activePropertyGroups = this.displayPropertyGroups.stream()
					.sorted()
					.collect(Collectors.joining(Constant.COMMA));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_ACTIVE_PROPERTY_GROUPS,
					StringUtils.isNotNullOrEmpty(activePropertyGroups) ? activePropertyGroups : Constant.NOT_AVAILABLE);
		} catch (Exception e) {
			logger.error("Failed to populate metadata information", e);
		}
	}

	/**
	 * Sends a GET request to {@code uri} and returns the raw {@link JsonNode}, or {@code null}
	 * if the device responded with no content (e.g. an empty body).
	 * <p>
	 * {@link FailedLoginException} and {@link ResourceNotReachableException} - the SDK's own typed
	 * signals for "bad credentials" and "device unreachable" respectively - are propagated as-is so
	 * Symphony's UI surfaces the accurate error.
	 *
	 * @param uri the API endpoint to call
	 * @return the raw response node, or {@code null} if the response body was empty
	 * @throws FailedLoginException          if authentication fails
	 * @throws ResourceNotReachableException if the device cannot be reached
	 */
	private JsonNode fetchJsonNode(String uri) throws FailedLoginException {
		try {
			return super.doGet(uri, JsonNode.class);
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			this.logger.error(Constant.FETCH_DATA_FAILED.formatted(uri, JsonNode.class.getName()), e);
			throw new IllegalStateException("Failed to send a request 'GET %s'".formatted(uri), e);
		}
	}

	/**
	 * Fetches JSON from {@code uri} and deserializes it into {@code targetClass}.
	 * <p>
	 * Returns {@code null} (with a warning log) if the device response is empty/no-content. This is
	 * treated as a confirmed, authoritative "no data" signal from the device rather than an error -
	 * callers should generally act on it (e.g. clear cached state), not just ignore it.
	 * <p>
	 * Throws {@link DataConversionException} if a response body was received but could not be
	 * deserialized into {@code targetClass} (malformed/unexpected payload) - this represents a
	 * genuine error, and callers should generally retain their previously cached value instead of
	 * overwriting it with incomplete data.
	 *
	 * @param <T>         the target type
	 * @param uri         the API endpoint to call
	 * @param targetClass the class to deserialize into
	 * @return the deserialized object, or {@code null} if the device returned no content
	 * @throws FailedLoginException    if the HTTP request itself fails
	 * @throws DataConversionException if the response body could not be deserialized
	 */
	private <T> T fetchAndConvert(String uri, Class<T> targetClass) throws FailedLoginException, DataConversionException {
		String responseClassName = targetClass.getName();
		JsonNode node = fetchJsonNode(uri);
		if (node == null) {
			this.logger.warn(Constant.FETCHED_DATA_NULL_WARNING.formatted(uri, responseClassName));
			return null;
		}
		try {
			return this.objectMapper.convertValue(node, targetClass);
		} catch (IllegalArgumentException e) {
			String message = Constant.CONVERT_DATA_FAILED.formatted(responseClassName);
			this.logger.error(message, e);
			throw new DataConversionException(message, e);
		}
	}

	/**
	 * Fetches and refreshes the cached active-alarms list from the device.
	 * <p>
	 * A {@code null} response (no content) or an empty/absent {@code alarms} array are both
	 * treated as an explicit, authoritative signal from the device that there are currently zero
	 * active alarms, and {@link #alarmsList} is reset to an empty list accordingly. This prevents
	 * Symphony from continuing to display a stale alarm count/details after all alarms have been
	 * cleared on the device.
	 * <p>
	 * If the top-level response cannot be parsed at all ({@link DataConversionException}), the
	 * previously cached {@link #alarmsList} is retained rather than risking data loss based on a
	 * malformed payload. The same applies per-alarm: if a specific alarm's detail response cannot
	 * be parsed, that alarm is skipped (logged) without discarding the rest of the refreshed list.
	 *
	 * @throws FailedLoginException if the HTTP request fails due to authentication or connectivity issues
	 */
	private void setupData() throws Exception {
		AlarmsResponse alarmsResponse;
		try {
			alarmsResponse = fetchAndConvert(Constant.ACTIVE_ALARMS_API, AlarmsResponse.class);
		} catch (DataConversionException e) {
			//	Malformed/unexpected payload - keep the previously cached alarms list.
			this.logger.warn("Retaining previously cached alarms list due to a malformed response from %s".formatted(Constant.ACTIVE_ALARMS_API), e);
			return;
		}

		List<Alarms> alarmRefs = alarmsResponse == null ? null : alarmsResponse.getAlarms();
		if (alarmRefs == null || alarmRefs.isEmpty()) {
			//	No content, or an empty/absent "alarms" array - the device is reporting zero active alarms.
			this.alarmsList = new ArrayList<>();
			return;
		}

		List<Alarms> fullAlarmsList = new ArrayList<>();
		for (Alarms alarm : alarmRefs) {
			try {
				Alarms fullAlarm = fetchAndConvert(Constant.ACTIVE_ALARMS_API + Constant.SLASH + alarm.getId(), Alarms.class);
				if (fullAlarm != null) {
					fullAlarmsList.add(fullAlarm);
				}
			} catch (DataConversionException e) {
				this.logger.error("Skipping alarm '%s' due to a malformed detail response.".formatted(alarm.getId()), e);
			}
		}
		this.alarmsList = fullAlarmsList;
	}

	/**
	 * Retrieves the device status from the remote endpoint and populates {@code stats}
	 * with properties from the {@code General} and {@code Network} groups.
	 *
	 * <p>If the status response is {@code null}, a warning is logged and both groups are skipped entirely.
	 *
	 * <p>{@link GeneralProperty} values are populated with a default fallback for {@code null} data;
	 * {@link NetworkProperty} values use a stricter fallback (see {@link Util#getDefaultValueForNullData(String, boolean)}).
	 *
	 * @param stats the map to populate with property display names as keys
	 * and their corresponding string values as values; must not be {@code null}
	 * @throws Exception if the status request fails for any other reason
	 */
	private void retrieveDeviceStatus(Map<String, String> stats) throws Exception {
		this.deviceStatus = this.fetchData(Status.class, Constant.STATUS_ENDPOINT);
		if (deviceStatus == null) {
			log.warn("The device status param is null; skip retrieving the General and Network group");
			return;
		}
		Arrays.stream(GeneralProperty.values()).forEach(property -> {
			var value = GeneralProperty.getPropertyValue(property, deviceStatus);
			stats.put(property.getDisplayName(), Util.getDefaultValueForNullData(value));
		});
		Arrays.stream(NetworkProperty.values()).forEach(property -> {
			var value = NetworkProperty.getPropertyValue(property, deviceStatus);
			stats.put(property.getDisplayName(), Util.getDefaultValueForNullData(value, false));
		});
	}

	/**
	 * Retrieves each optional call-statistics group whose name is present in {@link #displayPropertyGroups},
	 * populating {@code stats} with a {@code <GroupName>#<PropertyName>} entry per KPI. Groups not listed
	 * there are skipped entirely - no request is made for any of their KPIs.
	 *
	 * @param stats the map to populate with property display names as keys
	 * and their corresponding string values as values; must not be {@code null}
	 * @throws FailedLoginException if a KPI request fails due to authentication issues
	 */
	private void retrieveCallStats(Map<String, String> stats) throws FailedLoginException {
		retrieveKpiGroup(stats, Constant.CALL_LOAD_STATISTICS_GROUP, CallLoadStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_QUALITY_STATISTICS_GROUP, CallQualityStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_TERMINATION_STATISTICS_GROUP, CallTerminationStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP, CallMediaIssuesStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_CAPACITY_STATISTICS_GROUP, CallCapacityStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_ROUTING_STATISTICS_GROUP, CallRoutingStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_TRAFFIC_STATISTICS_GROUP, CallTrafficStatsProperty.values());
	}

	/**
	 * Retrieves every {@code property} belonging to {@code groupName} from the device's SBC call-statistics
	 * KPI endpoint, populating {@code stats} with a {@code <groupName>#<PropertyName>} entry for each - but
	 * only if {@code groupName} (or {@link Constant#CALL_STATS_ALL_GROUPS}) is present in
	 * {@link #displayPropertyGroups}; otherwise this is a no-op and no request is made.
	 * <p>
	 * Each KPI is fetched independently via its own request. A {@code null} response (no content) is
	 * treated as an authoritative zero. Any failure to fetch or parse a given KPI - a malformed response
	 * ({@link DataConversionException}) or any other error (e.g. the device not recognizing this particular
	 * {@code kpiId}) - is logged and reported as {@link Constant#NOT_AVAILABLE} for that single KPI, without
	 * affecting the rest of the group or aborting the poll cycle.
	 *
	 * @param <T>        the {@link KpiProperty} enum type for this group
	 * @param stats      the map to populate with property display names as keys
	 * @param groupName  the group name gating and prefixing these properties
	 * @param properties the KPI properties belonging to this group
	 * @throws FailedLoginException if a KPI request fails due to authentication issues
	 */
	private <T extends Enum<T> & KpiProperty> void retrieveKpiGroup(Map<String, String> stats, String groupName, T[] properties) throws FailedLoginException {
		if (!this.displayPropertyGroups.contains(Constant.CALL_STATS_ALL_GROUPS) && !this.displayPropertyGroups.contains(groupName)) {
			return;
		}
		for (T property : properties) {
			String uri = Constant.CALL_STATS_KPI_API + Constant.SLASH + property.getKpiId();
			String value;
			try {
				KpiValue kpi = fetchAndConvert(uri, KpiValue.class);
				value = kpi == null ? "0" : kpi.getValue();
			} catch (DataConversionException e) {
				this.logger.error("Failed to parse the '%s' KPI response from %s".formatted(property.getKpiId(), uri), e);
				value = null;
			} catch (FailedLoginException e) {
				throw e;
			} catch (Exception e) {
				this.logger.error("Failed to fetch the '%s' KPI from %s".formatted(property.getKpiId(), uri), e);
				value = null;
			}
			stats.put(groupName + Constant.HASH + property.getName(), Util.getDefaultValueForNullData(value, false));
		}
	}

	/**
	 * Sends a POST request to {@code uri} with {@code requestBody} and deserializes the response into {@code targetClass}.
	 * <p>
	 * {@link FailedLoginException} and {@link ResourceNotReachableException} are propagated as-is, matching
	 * {@link #fetchJsonNode(String)}'s contract.
	 *
	 * @param <T>         the target type
	 * @param uri         the API endpoint to call
	 * @param requestBody the request body to serialize and send
	 * @param targetClass the class to deserialize the response into
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails
	 */
	private <T> T postAndConvert(String uri, Object requestBody, Class<T> targetClass) throws FailedLoginException {
		try {
			return super.doPost(uri, requestBody, targetClass);
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			this.logger.error("Exception while posting data. Endpoint: %s".formatted(uri), e);
			throw new IllegalStateException("Failed to send a request 'POST %s'".formatted(uri), e);
		}
	}

	/**
	 * Populates {@code stats} with the current {@code CallDiagnostics} group values and appends the
	 * corresponding controls ({@code CalledNumber}/{@code CallingNumber}/{@code Destination} text
	 * fields and {@code Start}/{@code Stop} buttons) to {@code controls}. Like the KPI-heavy statistics
	 * groups, the whole {@code CallDiagnostics} group is gated behind {@link #displayPropertyGroups}
	 * (or {@link Constant#CALL_STATS_ALL_GROUPS}); if it isn't listed, this is a no-op and no request
	 * is made, even for a session already in progress.
	 * <p>
	 * If a test call session is currently tracked, its status is re-fetched from the device first
	 * (see {@link #refreshCallDiagnosticStatus()}) so that a call which disconnected on its own
	 * (rather than via the {@code Stop} control) is not reported as stale/{@code Connected} forever.
	 * <p>
	 * The {@code Stop} button/property is only included while there's actually something to stop
	 * (see {@link #isCallDiagnosticStoppable()}) - not before the first dial, and not once the call is over.
	 *
	 * @param stats    the map to populate with property display names as keys
	 * @param controls the list to append the {@code CallDiagnostics} controls to
	 * @throws FailedLoginException if refreshing the test call status fails due to authentication issues
	 */
	private void retrieveCallDiagnostics(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws FailedLoginException {
		if (!this.displayPropertyGroups.contains(Constant.CALL_STATS_ALL_GROUPS) && !this.displayPropertyGroups.contains(Constant.CALL_DIAGNOSTICS_GROUP)) {
			return;
		}
		if (this.callDiagnosticSessionId != null) {
			refreshCallDiagnosticStatus();
		}

		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
		String callIdKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALL_ID;
		String releaseCauseKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_RELEASE_CAUSE;
		String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
		String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

		stats.put(calledNumberKey, this.callDiagnosticCalledNumber);
		stats.put(callingNumberKey, this.callDiagnosticCallingNumber);
		stats.put(destinationKey, this.callDiagnosticDestination);
		stats.put(statusKey, this.callDiagnosticStatus);
		stats.put(callIdKey, this.callDiagnosticCallId);
		stats.put(releaseCauseKey, this.callDiagnosticReleaseCause);
		//	Buttons require a paired statistics entry to render, even though they carry no persistent value.
		stats.put(startKey, "");

		controls.add(ControllablePropertyFactory.createText(calledNumberKey, this.callDiagnosticCalledNumber));
		controls.add(ControllablePropertyFactory.createText(callingNumberKey, this.callDiagnosticCallingNumber));
		controls.add(ControllablePropertyFactory.createText(destinationKey, this.callDiagnosticDestination));
		controls.add(ControllablePropertyFactory.createButton(startKey, "Start", "Starting...", 0L));
		if (isCallDiagnosticStoppable()) {
			stats.put(stopKey, "");
			controls.add(ControllablePropertyFactory.createButton(stopKey, "Stop", "Stopping...", 0L));
		}
	}

	/**
	 * Re-fetches the current test call's status from the device, keeping {@link #callDiagnosticStatus},
	 * {@link #callDiagnosticCallId} and {@link #callDiagnosticReleaseCause} in sync rather than frozen
	 * at whatever they were when the call was dialed.
	 * <p>
	 * A {@code null}/no-content response, or any error other than a login failure (e.g. a 404 once the
	 * device has expired/forgotten the session), is treated as an authoritative signal that the call is
	 * over: {@link #callDiagnosticStatus} is reset to {@link Constant#CALL_DIAGNOSTICS_DISCONNECTED} and
	 * {@link #callDiagnosticSessionId} is cleared, same as a manual {@link #stopCallDiagnostic()}. Once
	 * the status is {@code Disconnected} - whether reported by the device or inferred here -
	 * {@link #callDiagnosticCallId} is reset to {@link Constant#NOT_AVAILABLE} too, since the call the id
	 * referred to is over; {@link #callDiagnosticReleaseCause} is left as-is so the reason for the
	 * disconnect (typically captured on the poll where the device first reported {@code Disconnected},
	 * before the session expired per the device's {@code keepResultTimeout}) remains visible. A
	 * malformed-but-present response is logged and otherwise ignored, leaving all previous values intact
	 * for this cycle.
	 *
	 * @throws FailedLoginException if authentication fails
	 */
	private void refreshCallDiagnosticStatus() throws FailedLoginException {
		String statusUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_STATUS_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		try {
			TestCallStatus status = fetchAndConvert(statusUri, TestCallStatus.class);
			if (status == null || status.getCallStatus() == null) {
				this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_DISCONNECTED;
				this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
				this.callDiagnosticSessionId = null;
				return;
			}
			this.callDiagnosticStatus = status.getCallStatus();
			this.callDiagnosticCallId = Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(this.callDiagnosticStatus)
					? Constant.NOT_AVAILABLE
					: Util.getDefaultValueForNullData(status.getCallId(), false);
			this.callDiagnosticReleaseCause = Util.getDefaultValueForNullData(status.getReleaseCause(), false);
		} catch (DataConversionException e) {
			this.logger.error("Failed to parse test call status response from %s".formatted(statusUri), e);
		} catch (FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			this.logger.warn("Failed to refresh test call status from %s; assuming the session has ended".formatted(statusUri), e);
			this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_DISCONNECTED;
			this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
			this.callDiagnosticSessionId = null;
		}
	}

	/**
	 * Dials a new SIP test call using the currently staged {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} values, then immediately
	 * delegates to {@link #refreshCallDiagnosticStatus()} to capture the initial state.
	 *
	 * @throws Exception if the dial request fails, or returns no session id
	 */
	private void startCallDiagnostic() throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("calledNumber", this.callDiagnosticCalledNumber);
		body.put("callingNumber", this.callDiagnosticCallingNumber);
		body.put("destAddress", this.callDiagnosticDestination);

		TestCallDialResponse dialResponse = postAndConvert(Constant.TEST_CALL_DIAL_API, body, TestCallDialResponse.class);
		if (dialResponse == null || dialResponse.getSessionId() == null) {
			throw new IllegalStateException("Test call dial request returned no session id");
		}
		this.callDiagnosticSessionId = String.valueOf(dialResponse.getSessionId());
		//	Clear out the previous call's residual callId/releaseCause so they aren't mistaken for this new call's data.
		this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
		this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;

		refreshCallDiagnosticStatus();
	}

	/**
	 * Drops the currently active test call session, if any, and resets the cached status/session id.
	 *
	 * @throws Exception if the drop request fails
	 */
	private void stopCallDiagnostic() throws Exception {
		if (this.callDiagnosticSessionId == null) {
			this.logger.warn("No active test call session to stop");
			return;
		}
		String dropUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_DROP_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		this.performDelete(dropUri);
		this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_DISCONNECTED;
		this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
		this.callDiagnosticSessionId = null;
	}

	/**
	 * Whether there's currently a test call to stop - i.e. {@link #callDiagnosticStatus} is neither the
	 * pre-dial sentinel ({@link Constant#CALL_DIAGNOSTICS_NOT_DIALED}) nor a terminal one
	 * ({@link Constant#CALL_DIAGNOSTICS_DISCONNECTED}). Gates whether the {@code Stop} button/property
	 * is shown.
	 *
	 * @return {@code true} if the {@code Stop} control should be shown
	 */
	private boolean isCallDiagnosticStoppable() {
		return !Constant.CALL_DIAGNOSTICS_NOT_DIALED.equals(this.callDiagnosticStatus)
				&& !Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(this.callDiagnosticStatus);
	}
}
