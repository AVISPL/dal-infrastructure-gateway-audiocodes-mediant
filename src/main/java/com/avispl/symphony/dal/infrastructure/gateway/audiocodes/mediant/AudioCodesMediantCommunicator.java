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
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.DataConversionException;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;
import com.avispl.symphony.dal.util.ControllablePropertyFactory;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.NetworkProperty;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallStatsProperty;
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
	 * Retrieves each {@link CallStatsProperty} KPI from the device's SBC call-statistics group
	 * and populates {@code stats} with a {@code CallStats#<name>} entry for each.
	 * <p>
	 * Each KPI is fetched independently via its own request. A {@code null} response (no
	 * content) is treated as an authoritative zero. A malformed response ({@link DataConversionException})
	 * is logged and reported as {@link Constant#NOT_AVAILABLE} without affecting the other KPIs.
	 *
	 * @param stats the map to populate with property display names as keys
	 * and their corresponding string values as values; must not be {@code null}
	 * @throws FailedLoginException if a KPI request fails due to authentication issues
	 */
	private void retrieveCallStats(Map<String, String> stats) throws FailedLoginException {
		for (CallStatsProperty property : CallStatsProperty.values()) {
			String uri = Constant.CALL_STATS_KPI_API + Constant.SLASH + property.getKpiId();
			String value;
			try {
				KpiValue kpi = fetchAndConvert(uri, KpiValue.class);
				value = kpi == null ? "0" : kpi.getValue();
			} catch (DataConversionException e) {
				this.logger.error("Failed to parse the '%s' KPI response from %s".formatted(property.getKpiId(), uri), e);
				value = null;
			}
			stats.put(Constant.CALL_STATS_GROUP + Constant.HASH + property.getName(), Util.getDefaultValueForNullData(value, false));
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
	 * fields and {@code Start}/{@code Stop} buttons) to {@code controls}.
	 * <p>
	 * This reflects adapter-side cached state only; no device request is made here. The test call's
	 * own status is refreshed at dial time (see {@link #startCallDiagnostic()}) rather than on every poll
	 * cycle, since the device does not offer a way to list/discover real active calls to poll against
	 * and the test call session is short-lived (see the device's {@code timeout}/{@code keepResultTimeout}
	 * defaults) relative to typical polling intervals.
	 *
	 * @param stats    the map to populate with property display names as keys
	 * @param controls the list to append the {@code CallDiagnostics} controls to
	 */
	private void retrieveCallDiagnostics(Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
		String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
		String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

		stats.put(calledNumberKey, this.callDiagnosticCalledNumber);
		stats.put(callingNumberKey, this.callDiagnosticCallingNumber);
		stats.put(destinationKey, this.callDiagnosticDestination);
		stats.put(statusKey, this.callDiagnosticStatus);
		//	Buttons require a paired statistics entry to render, even though they carry no persistent value.
		stats.put(startKey, "");
		stats.put(stopKey, "");

		controls.add(ControllablePropertyFactory.createText(calledNumberKey, this.callDiagnosticCalledNumber));
		controls.add(ControllablePropertyFactory.createText(callingNumberKey, this.callDiagnosticCallingNumber));
		controls.add(ControllablePropertyFactory.createText(destinationKey, this.callDiagnosticDestination));
		controls.add(ControllablePropertyFactory.createButton(startKey, "Start", "Starting...", 0L));
		controls.add(ControllablePropertyFactory.createButton(stopKey, "Stop", "Stopping...", 0L));
	}

	/**
	 * Dials a new SIP test call using the currently staged {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} values, then immediately checks its
	 * status once to capture the initial state (the session is guaranteed to still exist at this point,
	 * so no 404/expiry handling is needed here).
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

		String statusUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_STATUS_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		TestCallStatus status = fetchAndConvert(statusUri, TestCallStatus.class);
		this.callDiagnosticStatus = (status == null || status.getCallStatus() == null) ? Constant.NOT_AVAILABLE : status.getCallStatus();
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
		this.callDiagnosticSessionId = null;
	}
}
