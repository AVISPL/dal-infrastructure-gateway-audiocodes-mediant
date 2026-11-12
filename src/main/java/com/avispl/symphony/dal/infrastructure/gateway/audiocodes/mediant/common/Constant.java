/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import java.util.Set;

import lombok.experimental.UtilityClass;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallLoadStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallQualityStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallTrafficStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MediaDspStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MediaStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MemoryStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.RegistrationStatsProperty;

/**
 * Utility class that defines constant values used across the application.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@UtilityClass
public class Constant {
	//	Special characters
	public static final String HASH = "#";
	public static final String NOT_AVAILABLE = "N/A";
	public static final String UNDERSCORE = "_";
	public static final String SLASH = "/";
	public static final String COMMA = ", ";

	// Formats
	public static final String PROPERTY_FORMAT = "%s#%s";


	//	Fail messages
	public static final String FETCHED_DATA_NULL_WARNING = "Fetched data is null. Endpoint: %s, ResponseClass: %s";
	public static final String FETCH_DATA_FAILED = "Exception while fetching data. Endpoint: %s, ResponseClass: %s";
	public static final String CONVERT_DATA_FAILED = "Failed to convert response data to %s";

	public static final String ADAPTER_METADATA = "AdapterMetadata";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
	public static final String ADAPTER_ACTIVE_PROPERTY_GROUPS = "ActivePropertyGroups";
	//	Groups
	public static final String NETWORK_GROUP = "Network";

	//	API
	public final String STATUS_ENDPOINT = "/status";
	public static final String ACTIVE_ALARMS_API = "/alarms/active";
	public static final String ACTIVE_ALARM = "ActiveAlarms";
	//	Base paths for the device's singular-entity KPI scopes; a specific KPI id is appended
	//	(e.g. ".../busyCallsInTotal") per each *StatsProperty enum. Each constant is named after the
	//	device scope it addresses, which does not always match the display group it feeds - notably
	//	SBC_OTHER_STATS_KPI_API backs REGISTRATION_STATISTICS_GROUP.
	public static final String CALL_STATS_KPI_API = "/kpi/current/sbc/callStats/global";
	public static final String MEDIA_STATS_KPI_API = "/kpi/current/media/mediaStats/global";
	public static final String MEDIA_DSP_STATS_KPI_API = "/kpi/current/media/dspStats/global";
	public static final String MEDIA_CLUSTER_STATS_KPI_API = "/kpi/current/media/clusterStats/global";
	public static final String SBC_OTHER_STATS_KPI_API = "/kpi/current/sbc/otherStats/global";
	public static final String SIP_REC_STATS_KPI_API = "/kpi/current/sbc/sipRecStats/global";
	public static final String LICENSE_STATS_KPI_API = "/kpi/current/system/licenseStats/global";
	//	Device-wide memory entity, and the entity lists of the per-CPU and per-partition system KPIs; an entity index
	//	is appended to a list path (e.g. ".../cpu/0") to read every real-time KPI of that entity.
	public static final String SYSTEM_STATS_KPI_API = "/kpi/current/system/systemStats/global";
	public static final String CPU_STATS_KPI_API = "/kpi/current/system/cpuStats/cpu";
	public static final String STORAGE_STATS_KPI_API = "/kpi/current/system/storageStats/partition";
	//	History paths of the same entities; "/<kpiId>?interval=last" is appended (after the entity index for a
	//	per-CPU or per-partition KPI) to read the value of the last completed 15-minute interval.
	public static final String SYSTEM_STATS_HISTORY_API = "/kpi/history/system/systemStats/global";
	public static final String CPU_STATS_HISTORY_API = "/kpi/history/system/cpuStats/cpu";
	public static final String STORAGE_STATS_HISTORY_API = "/kpi/history/system/storageStats/partition";
	public static final String INTERVAL_PARAM = "interval";
	public static final String LAST_INTERVAL = "last";
	//	Base paths for the device's IP Group KPI entities; the IP Group index is appended (e.g. ".../ipGroup/1")
	//	to read every KPI of that IP Group. IP_GROUP_CALL_STATS_KPI_API on its own lists the IP Group entities.
	public static final String IP_GROUP_CALL_STATS_KPI_API = "/kpi/current/sbc/callStats/ipGroup";
	public static final String IP_GROUP_MEDIA_STATS_KPI_API = "/kpi/current/media/mediaStats/ipGroup";
	public static final String IP_GROUP_OTHER_STATS_KPI_API = "/kpi/current/sbc/otherStats/ipGroup";
	public static final String IP_GROUP_SIP_REC_STATS_KPI_API = "/kpi/current/sbc/sipRecStats/ipGroup";
	public static final String DETAILED_PARAM = "detailed";
	public static final String AFTER_PARAM = "after";
	//	Maximum number of KPI entity listing pages (IP Groups, CPUs, partitions) requested in one discovery.
	public static final int KPI_ENTITY_DISCOVERY_MAX_PAGES = 50;
	public static final String TEST_CALL_DIAL_API = "/sipTestCall/dial";
	public static final String TEST_CALL_STATUS_API = "/sipTestCall/getStatus";
	public static final String TEST_CALL_SHOW_API = "/sipTestCall/show";
	public static final String TEST_CALL_DROP_API = "/sipTestCall/drop";
	public static final String SESSION_ID_PARAM = "sessionId";

	//	ActiveAlarms summary group properties
	public static final String COUNT = "Count";
	public static final String SEVERITY = "Severity";
	public static final String SOURCES = "Sources";

	//	Groups
	public static final String CALL_DIAGNOSTICS_GROUP = "CallDiagnostics";
	//	Optional KPI-heavy call-statistics groups, gated behind displayPropertyGroups (see AudioCodesMediantCommunicator).
	//	If displayPropertyGroups contains this value, every group below is treated as enabled.
	public static final String CALL_STATS_ALL_GROUPS = "All";
	public static final String CALL_LOAD_STATISTICS_GROUP = "CallLoadStatistics";
	public static final String CALL_QUALITY_STATISTICS_GROUP = "CallQualityStatistics";
	public static final String CALL_TERMINATION_STATISTICS_GROUP = "CallTerminationStatistics";
	public static final String CALL_MEDIA_ISSUES_STATISTICS_GROUP = "CallMediaIssuesStatistics";
	public static final String CALL_CAPACITY_STATISTICS_GROUP = "CallCapacityStatistics";
	public static final String CALL_ROUTING_STATISTICS_GROUP = "CallRoutingStatistics";
	public static final String CALL_TRAFFIC_STATISTICS_GROUP = "CallTrafficStatistics";
	public static final String MEDIA_STATISTICS_GROUP = "MediaStatistics";
	public static final String MEDIA_DSP_STATISTICS_GROUP = "MediaDSPStatistics";
	public static final String MEDIA_CLUSTER_STATISTICS_GROUP = "MediaClusterStatistics";
	public static final String REGISTRATION_STATISTICS_GROUP = "RegistrationStatistics";
	public static final String SIP_REC_STATISTICS_GROUP = "SIPRecStatistics";
	public static final String LICENSE_STATISTICS_GROUP = "LicenseStatistics";
	public static final String MEMORY_STATISTICS_GROUP = "MemoryStatistics";
	//	Enable the per-CPU and per-partition statistics, reported as CPUStatistics_<index> and
	//	StorageStatistics_<index> groups.
	public static final String CPU_STATISTICS_GROUP = "CPUStatistics";
	public static final String STORAGE_STATISTICS_GROUP = "StorageStatistics";
	//	Per-CPU and per-partition property holding the entity name reported by the device.
	public static final String NAME = "Name";
	//	Enables the per-IP-Group statistics, reported as one IPGroupStatistics_<index>_<name> group per IP Group.
	public static final String IP_GROUP_STATISTICS_GROUP = "IPGroupStatistics";
	//	Every value displayPropertyGroups is allowed to contain; anything else is unsupported and dropped.
	public static final Set<String> SUPPORTED_PROPERTY_GROUPS = Set.of(
			CALL_STATS_ALL_GROUPS,
			CALL_DIAGNOSTICS_GROUP,
			CALL_LOAD_STATISTICS_GROUP,
			CALL_QUALITY_STATISTICS_GROUP,
			CALL_TERMINATION_STATISTICS_GROUP,
			CALL_MEDIA_ISSUES_STATISTICS_GROUP,
			CALL_CAPACITY_STATISTICS_GROUP,
			CALL_ROUTING_STATISTICS_GROUP,
			CALL_TRAFFIC_STATISTICS_GROUP,
			MEDIA_STATISTICS_GROUP,
			MEDIA_DSP_STATISTICS_GROUP,
			MEDIA_CLUSTER_STATISTICS_GROUP,
			REGISTRATION_STATISTICS_GROUP,
			SIP_REC_STATISTICS_GROUP,
			LICENSE_STATISTICS_GROUP,
			MEMORY_STATISTICS_GROUP,
			CPU_STATISTICS_GROUP,
			STORAGE_STATISTICS_GROUP,
			IP_GROUP_STATISTICS_GROUP,
			NETWORK_GROUP,
			ACTIVE_ALARM);

	//	Every value historicalProperties is allowed to contain; anything else is unsupported and dropped.
	//	Deliberately a subset of what the adapter emits - only the KPIs that make sense as a graphed time
	//	series - and sourced from the property enums so the names cannot drift from the emitted keys.
	public static final Set<String> SUPPORTED_HISTORICAL_PROPERTIES = Set.of(
			CallQualityStatsProperty.ANSWER_SEIZURE_RATIO.getName(),
			CallQualityStatsProperty.NETWORK_EFFECTIVENESS_RATIO.getName(),
			CallQualityStatsProperty.FAILED_CALLS_IN_RATIO.getName(),
			CallQualityStatsProperty.FAILED_CALLS_OUT_RATIO.getName(),
			CallQualityStatsProperty.POST_DIAL_DELAY.getName(),
			CallLoadStatsProperty.ACTIVE_SESSIONS.getName(),
			CallTrafficStatsProperty.ATTEMPTED_CALLS_RATE_IN.getName(),
			CallTrafficStatsProperty.ATTEMPTED_CALLS_RATE_OUT.getName(),
			MediaStatsProperty.MEDIA_PACKET_LOSS_IN.getName(),
			MediaStatsProperty.MEDIA_PACKET_LOSS_OUT.getName(),
			MediaStatsProperty.MEDIA_JITTER_IN.getName(),
			MediaStatsProperty.MEDIA_JITTER_OUT.getName(),
			MediaStatsProperty.MEDIA_BANDWIDTH_IN.getName(),
			MediaStatsProperty.MEDIA_BANDWIDTH_OUT.getName(),
			MediaDspStatsProperty.DSP_RESOURCE_CURRENT.getName(),
			RegistrationStatsProperty.REGISTERED_USERS.getName(),
			MemoryStatsProperty.MEMORY_UTILIZATION.getName());

	//	Per-group request interval, in seconds: how long a property group's data is reused before the group is requested
	//	again. Only the groups with a setter in GroupIntervalCommunicator accept one.
	public static final long DEFAULT_REQUEST_INTERVAL_SECONDS = 60L;
	public static final long MIN_REQUEST_INTERVAL_SECONDS = 60L;
	public static final long MAX_REQUEST_INTERVAL_SECONDS = 86400L;
	//	A group whose interval has passed to within this many milliseconds is requested anyway. getMultipleStatistics is
	//	called by Symphony on a schedule that can arrive slightly early (e.g. 59.9 seconds after the last cycle), and
	//	without this margin such a cycle would skip the group, doubling its effective interval.
	public static final long REQUEST_INTERVAL_TOLERANCE_MS = 5000L;
	//	Per-group property holding the time a group was last requested from the device, and the format of its value (UTC).
	public static final String DATA_RETRIEVED_TIME = "DataRetrievedTime";
	public static final String DATA_RETRIEVED_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	//	CallDiagnostics group properties
	public static final String CALL_DIAGNOSTICS_CALLED_NUMBER = "CalledNumber";
	public static final String CALL_DIAGNOSTICS_CALLING_NUMBER = "CallingNumber";
	public static final String CALL_DIAGNOSTICS_DESTINATION = "Destination";
	public static final String CALL_DIAGNOSTICS_STATUS = "Status";
	public static final String CALL_DIAGNOSTICS_START = "Start";
	public static final String CALL_DIAGNOSTICS_STOP = "Stop";
	public static final String CALL_DIAGNOSTICS_NOT_DIALED = "Not Dialed";
	public static final String CALL_DIAGNOSTICS_DISCONNECTED = "Disconnected";
	public static final String CALL_DIAGNOSTICS_CALL_ID = "CallID";
	public static final String CALL_DIAGNOSTICS_RELEASE_CAUSE = "ReleaseCause";

	//	Max lengths per the device's sipTestCall/dial contract
	public static final int CALL_DIAGNOSTICS_CALLED_NUMBER_MAX_LENGTH = 61;
	public static final int CALL_DIAGNOSTICS_CALLING_NUMBER_MAX_LENGTH = 61;
	public static final int CALL_DIAGNOSTICS_DESTINATION_MAX_LENGTH = 50;

}
