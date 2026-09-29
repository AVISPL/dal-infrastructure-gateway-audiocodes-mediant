/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Current live call/session volume KPIs (device group {@code sbc/callStats/global}), monitored
 * under the {@code CallLoadStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallLoadStatsProperty implements KpiProperty {
	BUSY_CALLS_IN_TOTAL("BusyCallsInTotal", "busyCallsInTotal"),
	BUSY_CALLS_OUT_TOTAL("BusyCallsOutTotal", "busyCallsOutTotal"),
	ACTIVE_SESSIONS("ActiveSessions", "activeSessions");

	private final String name;
	private final String kpiId;
}
