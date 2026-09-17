/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Routing/config-level rejection KPIs (device group {@code sbc/callStats/global}), monitored
 * under the {@code CallRoutingStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallRoutingStatsProperty implements KpiProperty {
	NO_ROUTE_CALLS_IN_TOTAL("NoRouteCallsInTotal", "noRouteCallsInTotal");

	private final String name;
	private final String kpiId;
}
