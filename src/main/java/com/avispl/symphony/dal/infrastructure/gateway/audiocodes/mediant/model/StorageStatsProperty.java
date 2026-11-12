/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Storage KPIs (device entity {@code system/storageStats/partition/<index>}), reported once for every storage
 * partition under a {@code StorageStatistics_<index>} group.
 * The {@code Avg} and {@code Max} KPIs are the average and maximum of the last completed 15-minute interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum StorageStatsProperty implements IntervalKpiProperty {
	STORAGE_UTILIZATION("StorageUtilization(%)", "storageUtilization", false),
	STORAGE_UTILIZATION_AVG("StorageUtilizationAvg(%)", "storageUtilizationAvg", true),
	STORAGE_UTILIZATION_MAX("StorageUtilizationMax(%)", "storageUtilizationMax", true),
	STORAGE_USAGE("StorageUsage(KB)", "storageUsage", false),
	STORAGE_USAGE_AVG("StorageUsageAvg(KB)", "storageUsageAvg", true),
	STORAGE_USAGE_MAX("StorageUsageMax(KB)", "storageUsageMax", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;
}
