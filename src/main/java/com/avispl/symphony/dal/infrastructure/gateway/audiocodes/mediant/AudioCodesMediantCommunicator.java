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
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.KpiValuesResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallConfig;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallDialResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallStatus;


/**
 * AudioCodesMediantCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class AudioCodesMediantCommunicator extends Communicator implements Monitorable, Controller {
	/**
	 * Pairs an optional call-statistics group with the KPI properties it displays.
	 *
	 * @param name the group name, gating and prefixing its properties
	 * @param properties the KPI properties belonging to the group
	 */
	private record KpiGroup(String name, List<KpiProperty> properties) {}

	/**
	 * The optional call-statistics groups, declared once so that the {@link #displayPropertyGroups} gate and
	 * the population loop cannot drift apart. All of these read from the same device scope
	 * ({@link Constant#CALL_STATS_KPI_API}), which is why one request per poll cycle serves every group.
	 */
	private static final List<KpiGroup> CALL_STATS_GROUPS = List.of(
			new KpiGroup(Constant.CALL_LOAD_STATISTICS_GROUP, List.of(CallLoadStatsProperty.values())),
			new KpiGroup(Constant.CALL_QUALITY_STATISTICS_GROUP, List.of(CallQualityStatsProperty.values())),
			new KpiGroup(Constant.CALL_TERMINATION_STATISTICS_GROUP, List.of(CallTerminationStatsProperty.values())),
			new KpiGroup(Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP, List.of(CallMediaIssuesStatsProperty.values())),
			new KpiGroup(Constant.CALL_CAPACITY_STATISTICS_GROUP, List.of(CallCapacityStatsProperty.values())),
			new KpiGroup(Constant.CALL_ROUTING_STATISTICS_GROUP, List.of(CallRoutingStatsProperty.values())),
			new KpiGroup(Constant.CALL_TRAFFIC_STATISTICS_GROUP, List.of(CallTrafficStatsProperty.values())));

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
	 * {@link Constant#CALL_DIAGNOSTICS_GROUP}. Defaults to {@link Constant#CALL_STATS_ALL_GROUPS},
	 * meaning every group is fetched unless the caller narrows the list - each KPI group is a
	 * per-KPI request against the device, and {@code CallDiagnostics} involves its own test-call
	 * session tracking, so groups the caller doesn't ask for are skipped entirely rather than
	 * fetched and hidden. Including {@link Constant#CALL_STATS_ALL_GROUPS} enables every group
	 * regardless of what else is listed.
	 */
	private List<String> displayPropertyGroups = new ArrayList<>(List.of(Constant.CALL_STATS_ALL_GROUPS));

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
		clearCallDiagnosticConfig();
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
			Map<String, String> dynamicStats = new HashMap<>();
			List<AdvancedControllableProperty> controls = new ArrayList<>();
			this.setupData();
			retrieveMetadata(stats);
			retrieveDeviceStatus(stats);
			retrieveCallStats(stats, dynamicStats);
			retrieveCallDiagnostics(stats, controls);
			stats.putAll(Util.generateActiveAlarmsProperties(this.alarmsList));
			this.localExtendedStatistics.setStatistics(stats);
			this.localExtendedStatistics.setDynamicStatistics(dynamicStats);
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
	 * populating {@code stats}/{@code dynamicStats} with a {@code <GroupName>#<PropertyName>} entry per KPI
	 * (see {@link Util#putKpiValue}). Groups not listed there are skipped entirely, and if no group at all is
	 * enabled no request is made.
	 * <p>
	 * Every group reads from the same device scope, so the whole scope is fetched <em>once</em> per poll cycle
	 * and shared across all of them rather than issuing a request per KPI.
	 *
	 * @param stats the map to populate with the regular (non-trended) properties; must not be {@code null}
	 * @param dynamicStats the map to populate with the gauge properties; must not be {@code null}
	 * @throws FailedLoginException if the KPI request fails due to authentication issues
	 */
	private void retrieveCallStats(Map<String, String> stats, Map<String, String> dynamicStats) throws FailedLoginException {
		List<KpiGroup> enabledGroups = CALL_STATS_GROUPS.stream()
				.filter(group -> isGroupEnabled(group.name()))
				.toList();
		if (enabledGroups.isEmpty()) {
			return;
		}
		Map<String, String> kpiValues = fetchCallStatsScope();
		for (KpiGroup group : enabledGroups) {
			populateKpiGroup(stats, dynamicStats, group, kpiValues);
		}
	}

	/**
	 * Fetches the whole SBC call-statistics KPI scope in a single request and indexes it by {@code kpiId}.
	 * <p>
	 * Returns an empty map - rather than throwing - for every non-authentication failure mode: a
	 * {@code 204 No Content} response (which the device returns for a scope absent on this hardware
	 * variant), a malformed payload ({@link DataConversionException}), or any other error. Callers then
	 * resolve every KPI to {@link Constant#NOT_AVAILABLE} uniformly, so an unavailable scope degrades the
	 * call-statistics groups without aborting the poll cycle or affecting any other group.
	 * <p>
	 * Note that this couples the fate of all call-statistics KPIs together: a single failed request now
	 * blanks every KPI in every enabled group, where the previous per-KPI fetch could degrade one KPI in
	 * isolation. That is the trade for collapsing the request count from one-per-KPI to one-per-cycle.
	 *
	 * @return {@code kpiId} to raw value, or an empty map if the scope could not be read
	 * @throws FailedLoginException if the request fails due to authentication issues
	 */
	private Map<String, String> fetchCallStatsScope() throws FailedLoginException {
		try {
			KpiValuesResponse response = fetchAndConvert(Constant.CALL_STATS_KPI_API, KpiValuesResponse.class);
			if (response == null || response.getItems() == null) {
				this.logger.warn("No call statistics KPIs returned from %s; reporting them as unavailable".formatted(Constant.CALL_STATS_KPI_API));
				return Map.of();
			}
			//	Built by hand rather than Collectors.toMap: a duplicate id in the response would make the
			//	latter throw, turning a benign device quirk into a total failure of every KPI group.
			Map<String, String> kpiValues = new HashMap<>();
			for (KpiValue item : response.getItems()) {
				if (item.getId() != null) {
					kpiValues.put(item.getId(), item.getValue());
				}
			}
			return kpiValues;
		} catch (DataConversionException e) {
			this.logger.error("Failed to parse the call statistics KPI response from %s".formatted(Constant.CALL_STATS_KPI_API), e);
			return Map.of();
		} catch (FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			this.logger.error("Failed to fetch the call statistics KPIs from %s".formatted(Constant.CALL_STATS_KPI_API), e);
			return Map.of();
		}
	}

	/**
	 * Populates {@code stats}/{@code dynamicStats} with a {@code <groupName>#<PropertyName>} entry for every
	 * property in {@code group}, resolving each one by {@code kpiId} against an already-fetched scope.
	 * <p>
	 * A {@code kpiId} absent from {@code kpiValues} - because the device does not report it, or because the
	 * scope could not be read at all - is handled exactly like a {@code null} value: reported as
	 * {@link Constant#NOT_AVAILABLE} in {@code stats}, and omitted from {@code dynamicStats}.
	 *
	 * @param stats the map to populate with the regular (non-trended) properties
	 * @param dynamicStats the map to populate with the gauge properties
	 * @param group the group being populated, supplying the key prefix and its KPI properties
	 * @param kpiValues the fetched scope, indexed by {@code kpiId}
	 */
	private void populateKpiGroup(Map<String, String> stats, Map<String, String> dynamicStats, KpiGroup group, Map<String, String> kpiValues) {
		for (KpiProperty property : group.properties()) {
			String propertyKey = group.name() + Constant.HASH + property.getName();
			Util.putKpiValue(stats, dynamicStats, propertyKey, kpiValues.get(property.getKpiId()), property.isGauge());
		}
	}

	/**
	 * Whether {@code groupName} should be pulled from the device and displayed - i.e. it is listed in
	 * {@link #displayPropertyGroups}, either by name or via {@link Constant#CALL_STATS_ALL_GROUPS}.
	 *
	 * @param groupName the group to test
	 * @return {@code true} if the group is enabled
	 */
	private boolean isGroupEnabled(String groupName) {
		return this.displayPropertyGroups.contains(Constant.CALL_STATS_ALL_GROUPS) || this.displayPropertyGroups.contains(groupName);
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
		if (!isGroupEnabled(Constant.CALL_DIAGNOSTICS_GROUP)) {
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
	 * before the session expired per the device's {@code keepResultTimeout}) remains visible.
	 * {@link #callDiagnosticCalledNumber}/{@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination}
	 * are deliberately left untouched here too - once a call ends they keep showing what was dialed
	 * until the caller stages new values or the adapter is destroyed, rather than blanking out on
	 * disconnect. A malformed-but-present response is logged and otherwise ignored, leaving all previous
	 * values intact for this cycle.
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
			if (Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(this.callDiagnosticStatus)) {
				this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
			} else {
				this.callDiagnosticCallId = Util.getDefaultValueForNullData(status.getCallId(), false);
				refreshCallDiagnosticConfig();
			}
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
	 * Fetches the device's own record of the dialed-call parameters ({@code calledNumber}/
	 * {@code callingNumber}/{@code destAddress}) for the current test call session via
	 * {@code GET /sipTestCall/show}, and updates {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} to match, so these
	 * reflect the device's authoritative data rather than staying frozen at whatever was staged
	 * locally before {@code Start} was pressed. A missing/malformed response leaves the
	 * previously known values intact for this cycle rather than blanking them out.
	 *
	 * @throws FailedLoginException if the request fails due to authentication issues
	 */
	private void refreshCallDiagnosticConfig() throws FailedLoginException {
		String showUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_SHOW_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		try {
			TestCallConfig config = fetchAndConvert(showUri, TestCallConfig.class);
			if (config == null) {
				return;
			}
			this.callDiagnosticCalledNumber = config.getCalledNumber() != null ? config.getCalledNumber() : "";
			this.callDiagnosticCallingNumber = config.getCallingNumber() != null ? config.getCallingNumber() : "";
			this.callDiagnosticDestination = config.getDestAddress() != null ? config.getDestAddress() : "";
		} catch (DataConversionException e) {
			this.logger.error("Failed to parse test call config response from %s".formatted(showUri), e);
		} catch (FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			this.logger.warn("Failed to refresh test call config from %s; keeping previously known values".formatted(showUri), e);
		}
	}

	/**
	 * Blanks the staged/echoed {@code CalledNumber}/{@code CallingNumber}/{@code Destination}
	 * values. Only called from {@link #internalDestroy()} - once a test call ends, these are
	 * deliberately left in place (reflecting the last dialed/echoed values) until the caller
	 * stages new ones via {@link #controlProperty} or the adapter instance is destroyed.
	 */
	private void clearCallDiagnosticConfig() {
		this.callDiagnosticCalledNumber = "";
		this.callDiagnosticCallingNumber = "";
		this.callDiagnosticDestination = "";
	}

	/**
	 * Dials a new SIP test call using the currently staged {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} values, then immediately
	 * delegates to {@link #refreshCallDiagnosticStatus()} to capture the initial state.
	 * <p>
	 * Per the device's {@code POST /sipTestCall/dial} contract, {@code calledNumber} and
	 * {@code callingNumber} are always required, and a destination is required via either
	 * {@code destAddress} or {@code destIpGroup} - this adapter only exposes the former
	 * ({@code CallDiagnostic#Destination}), so all three staged fields are required here.
	 *
	 * @throws IllegalArgumentException if any of the three staged fields is blank
	 * @throws Exception                if the dial request fails, or returns no session id
	 */
	private void startCallDiagnostic() throws Exception {
		if (StringUtils.isNullOrEmpty(this.callDiagnosticCalledNumber, true)
				|| StringUtils.isNullOrEmpty(this.callDiagnosticCallingNumber, true)
				|| StringUtils.isNullOrEmpty(this.callDiagnosticDestination, true)) {
			throw new IllegalArgumentException("CalledNumber, CallingNumber and Destination must all be set before starting a test call");
		}
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
