/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import java.util.Set;

import lombok.experimental.UtilityClass;

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
	public static final String MEDIA_DSP_STATISTICS_GROUP = "MediaDspStatistics";
	public static final String MEDIA_CLUSTER_STATISTICS_GROUP = "MediaClusterStatistics";
	public static final String REGISTRATION_STATISTICS_GROUP = "RegistrationStatistics";
	public static final String SIP_REC_STATISTICS_GROUP = "SipRecStatistics";
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
			NETWORK_GROUP,
			ACTIVE_ALARM);

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
