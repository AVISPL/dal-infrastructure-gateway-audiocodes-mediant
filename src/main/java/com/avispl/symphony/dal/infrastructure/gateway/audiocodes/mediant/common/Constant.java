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
	public static final String NONE_VALUE = "None";
	public static final String NULL = "Null";
	public static final String NOT_AVAILABLE = "N/A";

	//	Groups
	public static final String NETWORK_GROUP = "Network";

	//	API
	public final String STATUS_ENDPOINT = "/status";

	public static final String ADAPTER_METADATA = "AdapterMetadata";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
}
