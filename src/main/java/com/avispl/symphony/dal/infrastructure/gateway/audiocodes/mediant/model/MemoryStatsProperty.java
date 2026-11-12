/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Memory KPIs (device entity {@code system/systemStats/global}), monitored under the {@code MemoryStatistics} group.
 * The {@code Avg} and {@code Max} KPIs are the average and maximum of the last completed 15-minute interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MemoryStatsProperty implements IntervalKpiProperty {
	MEMORY_UTILIZATION("MemoryUtilization(%)", "memoryUtilization", false),
	MEMORY_AVERAGE_UTILIZATION("MemoryAverageUtilization(%)", "memoryUtilizationAvg", true),
	MEMORY_MAXIMUM_UTILIZATION("MemoryMaximumUtilization(%)", "memoryUtilizationMax", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;
}
