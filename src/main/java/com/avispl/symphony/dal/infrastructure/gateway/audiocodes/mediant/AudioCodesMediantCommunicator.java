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

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.EndpointStatistics;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.Communicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.NetworkProperty;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.Alarms;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.AlarmsResponse;


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
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			Map<String, String> stats = new HashMap<>();
			this.setupData();
			retrieveMetadata(stats);
			retrieveDeviceStatus(stats);
			stats.putAll(Util.generateActiveAlarmsProperties(this.alarmsList));
			this.localExtendedStatistics.setStatistics(stats);
		} finally {
			reentrantLock.unlock();
		}
		return List.of(this.localExtendedStatistics, this.localEndpointStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		throw new UnsupportedOperationException("Currently, property control is not supported.");
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		throw new UnsupportedOperationException("Currently, the feature to control properties is not supported.");
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
	 *
	 * @param uri the API endpoint to call
	 * @return the raw response node, or {@code null} if the response body was empty
	 * @throws FailedLoginException if the HTTP request itself fails
	 */
	private JsonNode fetchJsonNode(String uri) throws FailedLoginException {
		try {
			return super.doGet(uri, JsonNode.class);
		} catch (Exception e) {
			this.logger.error(Constant.FETCH_DATA_FAILED.formatted(uri, JsonNode.class.getName()));
			throw new FailedLoginException(Constant.LOGIN_FAILED + ": " + e.getMessage());
		}
	}

	/**
	 * Fetches JSON from {@code uri} and deserializes it into {@code targetClass}.
	 * Returns {@code null} (with a warning log) if the response is missing or cannot be deserialized,
	 * so the caller can safely retain the previously cached value.
	 *
	 * @param <T>         the target type
	 * @param uri         the API endpoint to call
	 * @param targetClass the class to deserialize into
	 * @return the deserialized object, or {@code null} if unavailable
	 * @throws FailedLoginException if the HTTP request itself fails
	 */
	private <T> T fetchAndConvert(String uri, Class<T> targetClass) throws FailedLoginException {
		String responseClassName = targetClass.getName();
		JsonNode node = fetchJsonNode(uri);
		if (node == null) {
			this.logger.warn(Constant.FETCHED_DATA_NULL_WARNING.formatted(uri, responseClassName));
			return null;
		}
		try {
			return this.objectMapper.convertValue(node, targetClass);
		} catch (IllegalArgumentException e) {
			this.logger.error(Constant.CONVERT_DATA_FAILED.formatted(responseClassName));
			return null;
		}
	}

	/**
	 * Fetches and refreshes the cached active-alarms list from the device.
	 * <p>
	 * Unlike a generic fetch failure (network error, or a response that cannot be parsed at all —
	 * in which case the previously cached {@link #alarmsList} is retained), a <b>no-content
	 * response</b> or an <b>empty/absent {@code alarms} array</b> are both treated as an explicit,
	 * authoritative signal from the device that there are currently zero active alarms, and
	 * {@link #alarmsList} is reset to an empty list accordingly. This prevents Symphony from
	 * continuing to display a stale alarm count/details after all alarms have been cleared on the
	 * device.
	 *
	 * @throws FailedLoginException if the HTTP request fails due to authentication or connectivity issues
	 */
	private void setupData() throws Exception {
		JsonNode alarmsNode = fetchJsonNode(Constant.ACTIVE_ALARMS_API);
		if (alarmsNode == null) {
			//	No content returned - the device is reporting zero active alarms.
			this.logger.debug("No content returned for %s; treating as zero active alarms.".formatted(Constant.ACTIVE_ALARMS_API));
			this.alarmsList = new ArrayList<>();
			return;
		}

		AlarmsResponse alarmsResponse;
		try {
			alarmsResponse = this.objectMapper.convertValue(alarmsNode, AlarmsResponse.class);
		} catch (IllegalArgumentException e) {
			//	Response could not be parsed at all - keep the previously cached alarms list rather
			//	than risk wiping valid data based on an unexpected/malformed payload.
			this.logger.error(Constant.CONVERT_DATA_FAILED.formatted(AlarmsResponse.class.getName()));
			return;
		}

		List<Alarms> alarmRefs = alarmsResponse.getAlarms();
		if (alarmRefs == null || alarmRefs.isEmpty()) {
			//	Empty (or absent) "alarms" array - the device is reporting zero active alarms.
			this.alarmsList = new ArrayList<>();
			return;
		}

		List<Alarms> fullAlarmsList = new ArrayList<>();
		for (Alarms alarm : alarmRefs) {
			Alarms fullAlarm = fetchAndConvert(Constant.ACTIVE_ALARMS_API + Constant.SLASH + alarm.getId(), Alarms.class);
			if (fullAlarm != null) {
				fullAlarmsList.add(fullAlarm);
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
}
