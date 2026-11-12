/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CPU KPIs (device entity {@code system/cpuStats/cpu/<index>}), reported once for every CPU under a
 * {@code CPUStatistics_<index>} group.
 * The {@code Avg} and {@code Max} KPIs are the average and maximum of the last completed 15-minute interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CpuStatsProperty implements IntervalKpiProperty {
	CPU_UTILIZATION("CPUUtilization(%)", "cpuUtilization", false),
	CPU_UTILIZATION_AVG("CPUUtilizationAvg(%)", "cpuUtilizationAvg", true),
	CPU_UTILIZATION_MAX("CPUUtilizationMax(%)", "cpuUtilizationMax", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;
}
