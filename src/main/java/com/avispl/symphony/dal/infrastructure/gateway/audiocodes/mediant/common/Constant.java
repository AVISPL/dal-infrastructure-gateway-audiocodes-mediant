/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

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
	//	Groups
	public static final String NETWORK_GROUP = "Network";

	//	API
	public final String STATUS_ENDPOINT = "/status";
	public static final String ACTIVE_ALARMS_API = "/alarms/active";
	public static final String ACTIVE_ALARM = "ActiveAlarms";

	//	ActiveAlarms summary group properties
	public static final String COUNT = "Count";
	public static final String SEVERITY = "Severity";
	public static final String SOURCES = "Sources";

}
