/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Cluster-wide DSP and media utilisation KPIs (device group {@code media/clusterStats/global}),
 * monitored under the {@code MediaClusterStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MediaClusterStatsProperty implements KpiProperty {
	DSP_CLUSTER_UTILIZATION("DSPClusterUtilization(%)", "dspClusterUtilization"),
	MEDIA_CLUSTER_UTILIZATION("MediaClusterUtilization(%)", "mediaClusterUtilization");

	private final String name;
	private final String kpiId;
}
