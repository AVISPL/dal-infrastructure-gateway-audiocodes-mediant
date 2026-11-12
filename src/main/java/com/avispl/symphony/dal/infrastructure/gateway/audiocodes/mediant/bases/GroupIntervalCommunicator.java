/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Communicator that holds one request interval per cacheable property group.
 * <p>
 * The interval of a group is the number of seconds its data is reused before the group is requested from the device
 * again. It is set through the {@code <groupName>RequestInterval} properties, as a whole number of seconds from
 * {@link Constant#MIN_REQUEST_INTERVAL_SECONDS} to {@link Constant#MAX_REQUEST_INTERVAL_SECONDS}. A group with no
 * interval set is requested at every monitoring cycle and is not cached.
 * <p>
 * General and AdapterMetadata are always requested. Network and CallDiagnostics have no interval property on purpose:
 * Network is read from the same {@code /status} response as General, so caching it saves no request, and
 * CallDiagnostics tracks a live test call driven by controls, so caching it would show a stale call state.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public abstract class GroupIntervalCommunicator extends Communicator {
	/** Interval in seconds by property group name, for the groups whose interval property has been set. */
	private final Map<String, Long> requestIntervalSeconds = new ConcurrentHashMap<>();

	/**
	 * Returns the request interval of a property group.
	 *
	 * @param group the property group name
	 * @return the interval in seconds; {@link Constant#DEFAULT_REQUEST_INTERVAL_SECONDS} if none has been set
	 */
	protected final long getRequestIntervalSeconds(String group) {
		return requestIntervalSeconds.getOrDefault(group, Constant.DEFAULT_REQUEST_INTERVAL_SECONDS);
	}

	/**
	 * Checks whether the interval property of a property group has been set to a non-blank value.
	 *
	 * @param group the property group name
	 * @return {@code true} if the group has an interval set, and is therefore cached
	 */
	protected final boolean isRequestIntervalConfigured(String group) {
		return requestIntervalSeconds.containsKey(group);
	}

	/**
	 * Returns the property groups whose interval property has been set.
	 *
	 * @return the group names
	 */
	protected final Set<String> getIntervalConfiguredGroups() {
		return Set.copyOf(requestIntervalSeconds.keySet());
	}

	private String readInterval(String group) {
		return String.valueOf(getRequestIntervalSeconds(group));
	}

	/**
	 * Stores the interval of a property group. A blank value removes the stored interval, so the group is requested at
	 * every cycle again; a non-numeric value or one outside {@link Constant#MIN_REQUEST_INTERVAL_SECONDS} to
	 * {@link Constant#MAX_REQUEST_INTERVAL_SECONDS} stores {@link Constant#DEFAULT_REQUEST_INTERVAL_SECONDS} and logs a
	 * warning.
	 *
	 * @param property the name of the interval property, used in log messages
	 * @param group the property group name
	 * @param value the interval in seconds, as a string
	 */
	private void writeInterval(String property, String group, String value) {
		if (StringUtils.isNullOrEmpty(value, true)) {
			requestIntervalSeconds.remove(group);
			return;
		}
		long parsed = -1;
		try {
			parsed = Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			logger.warn("Invalid %s '%s': %s".formatted(property, value, e.getMessage()));
		}
		if (parsed < Constant.MIN_REQUEST_INTERVAL_SECONDS || parsed > Constant.MAX_REQUEST_INTERVAL_SECONDS) {
			logger.warn("Invalid %s '%s'; the valid range is %d to %d seconds, using %d".formatted(property, value,
					Constant.MIN_REQUEST_INTERVAL_SECONDS, Constant.MAX_REQUEST_INTERVAL_SECONDS, Constant.DEFAULT_REQUEST_INTERVAL_SECONDS));
			parsed = Constant.DEFAULT_REQUEST_INTERVAL_SECONDS;
		}
		requestIntervalSeconds.put(group, parsed);
	}

	/**
	 * Returns the request interval of the {@code ActiveAlarms} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getActiveAlarmsRequestInterval() {
		return readInterval(Constant.ACTIVE_ALARM);
	}

	/**
	 * Sets the request interval of the {@code ActiveAlarms} property group, in seconds.
	 *
	 * @param activeAlarmsRequestInterval the interval in seconds, as a string
	 */
	public void setActiveAlarmsRequestInterval(String activeAlarmsRequestInterval) {
		writeInterval("activeAlarmsRequestInterval", Constant.ACTIVE_ALARM, activeAlarmsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallLoadStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallLoadStatisticsRequestInterval() {
		return readInterval(Constant.CALL_LOAD_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallLoadStatistics} property group, in seconds.
	 *
	 * @param callLoadStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallLoadStatisticsRequestInterval(String callLoadStatisticsRequestInterval) {
		writeInterval("callLoadStatisticsRequestInterval", Constant.CALL_LOAD_STATISTICS_GROUP, callLoadStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallQualityStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallQualityStatisticsRequestInterval() {
		return readInterval(Constant.CALL_QUALITY_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallQualityStatistics} property group, in seconds.
	 *
	 * @param callQualityStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallQualityStatisticsRequestInterval(String callQualityStatisticsRequestInterval) {
		writeInterval("callQualityStatisticsRequestInterval", Constant.CALL_QUALITY_STATISTICS_GROUP, callQualityStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallTerminationStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallTerminationStatisticsRequestInterval() {
		return readInterval(Constant.CALL_TERMINATION_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallTerminationStatistics} property group, in seconds.
	 *
	 * @param callTerminationStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallTerminationStatisticsRequestInterval(String callTerminationStatisticsRequestInterval) {
		writeInterval("callTerminationStatisticsRequestInterval", Constant.CALL_TERMINATION_STATISTICS_GROUP, callTerminationStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallMediaIssuesStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallMediaIssuesStatisticsRequestInterval() {
		return readInterval(Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallMediaIssuesStatistics} property group, in seconds.
	 *
	 * @param callMediaIssuesStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallMediaIssuesStatisticsRequestInterval(String callMediaIssuesStatisticsRequestInterval) {
		writeInterval("callMediaIssuesStatisticsRequestInterval", Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP, callMediaIssuesStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallCapacityStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallCapacityStatisticsRequestInterval() {
		return readInterval(Constant.CALL_CAPACITY_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallCapacityStatistics} property group, in seconds.
	 *
	 * @param callCapacityStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallCapacityStatisticsRequestInterval(String callCapacityStatisticsRequestInterval) {
		writeInterval("callCapacityStatisticsRequestInterval", Constant.CALL_CAPACITY_STATISTICS_GROUP, callCapacityStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallRoutingStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallRoutingStatisticsRequestInterval() {
		return readInterval(Constant.CALL_ROUTING_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallRoutingStatistics} property group, in seconds.
	 *
	 * @param callRoutingStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallRoutingStatisticsRequestInterval(String callRoutingStatisticsRequestInterval) {
		writeInterval("callRoutingStatisticsRequestInterval", Constant.CALL_ROUTING_STATISTICS_GROUP, callRoutingStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CallTrafficStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCallTrafficStatisticsRequestInterval() {
		return readInterval(Constant.CALL_TRAFFIC_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CallTrafficStatistics} property group, in seconds.
	 *
	 * @param callTrafficStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCallTrafficStatisticsRequestInterval(String callTrafficStatisticsRequestInterval) {
		writeInterval("callTrafficStatisticsRequestInterval", Constant.CALL_TRAFFIC_STATISTICS_GROUP, callTrafficStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code MediaStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getMediaStatisticsRequestInterval() {
		return readInterval(Constant.MEDIA_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code MediaStatistics} property group, in seconds.
	 *
	 * @param mediaStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setMediaStatisticsRequestInterval(String mediaStatisticsRequestInterval) {
		writeInterval("mediaStatisticsRequestInterval", Constant.MEDIA_STATISTICS_GROUP, mediaStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code MediaDSPStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getMediaDspStatisticsRequestInterval() {
		return readInterval(Constant.MEDIA_DSP_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code MediaDSPStatistics} property group, in seconds.
	 *
	 * @param mediaDspStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setMediaDspStatisticsRequestInterval(String mediaDspStatisticsRequestInterval) {
		writeInterval("mediaDspStatisticsRequestInterval", Constant.MEDIA_DSP_STATISTICS_GROUP, mediaDspStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code MediaClusterStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getMediaClusterStatisticsRequestInterval() {
		return readInterval(Constant.MEDIA_CLUSTER_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code MediaClusterStatistics} property group, in seconds.
	 *
	 * @param mediaClusterStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setMediaClusterStatisticsRequestInterval(String mediaClusterStatisticsRequestInterval) {
		writeInterval("mediaClusterStatisticsRequestInterval", Constant.MEDIA_CLUSTER_STATISTICS_GROUP, mediaClusterStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code RegistrationStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getRegistrationStatisticsRequestInterval() {
		return readInterval(Constant.REGISTRATION_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code RegistrationStatistics} property group, in seconds.
	 *
	 * @param registrationStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setRegistrationStatisticsRequestInterval(String registrationStatisticsRequestInterval) {
		writeInterval("registrationStatisticsRequestInterval", Constant.REGISTRATION_STATISTICS_GROUP, registrationStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code SIPRecStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getSipRecStatisticsRequestInterval() {
		return readInterval(Constant.SIP_REC_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code SIPRecStatistics} property group, in seconds.
	 *
	 * @param sipRecStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setSipRecStatisticsRequestInterval(String sipRecStatisticsRequestInterval) {
		writeInterval("sipRecStatisticsRequestInterval", Constant.SIP_REC_STATISTICS_GROUP, sipRecStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code LicenseStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getLicenseStatisticsRequestInterval() {
		return readInterval(Constant.LICENSE_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code LicenseStatistics} property group, in seconds.
	 *
	 * @param licenseStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setLicenseStatisticsRequestInterval(String licenseStatisticsRequestInterval) {
		writeInterval("licenseStatisticsRequestInterval", Constant.LICENSE_STATISTICS_GROUP, licenseStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code MemoryStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getMemoryStatisticsRequestInterval() {
		return readInterval(Constant.MEMORY_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code MemoryStatistics} property group, in seconds.
	 *
	 * @param memoryStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setMemoryStatisticsRequestInterval(String memoryStatisticsRequestInterval) {
		writeInterval("memoryStatisticsRequestInterval", Constant.MEMORY_STATISTICS_GROUP, memoryStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code CPUStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getCpuStatisticsRequestInterval() {
		return readInterval(Constant.CPU_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code CPUStatistics} property group, in seconds. The interval covers the CPU listing
	 * and the statistics of every CPU.
	 *
	 * @param cpuStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setCpuStatisticsRequestInterval(String cpuStatisticsRequestInterval) {
		writeInterval("cpuStatisticsRequestInterval", Constant.CPU_STATISTICS_GROUP, cpuStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code StorageStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getStorageStatisticsRequestInterval() {
		return readInterval(Constant.STORAGE_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code StorageStatistics} property group, in seconds. The interval covers the partition
	 * listing and the statistics of every partition.
	 *
	 * @param storageStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setStorageStatisticsRequestInterval(String storageStatisticsRequestInterval) {
		writeInterval("storageStatisticsRequestInterval", Constant.STORAGE_STATISTICS_GROUP, storageStatisticsRequestInterval);
	}

	/**
	 * Returns the request interval of the {@code IPGroupStatistics} property group.
	 *
	 * @return the interval in seconds, as a string
	 */
	public String getIpGroupStatisticsRequestInterval() {
		return readInterval(Constant.IP_GROUP_STATISTICS_GROUP);
	}

	/**
	 * Sets the request interval of the {@code IPGroupStatistics} property group, in seconds. The interval covers the
	 * IP Group listing and the statistics of every IP Group.
	 *
	 * @param ipGroupStatisticsRequestInterval the interval in seconds, as a string
	 */
	public void setIpGroupStatisticsRequestInterval(String ipGroupStatisticsRequestInterval) {
		writeInterval("ipGroupStatisticsRequestInterval", Constant.IP_GROUP_STATISTICS_GROUP, ipGroupStatisticsRequestInterval);
	}
}
