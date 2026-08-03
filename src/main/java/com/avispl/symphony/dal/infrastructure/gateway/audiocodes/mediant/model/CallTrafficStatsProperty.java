/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Attempted call volume and caller-side outcome KPIs (device group {@code sbc/callStats/global}),
 * monitored under the {@code CallTrafficStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallTrafficStatsProperty implements KpiProperty {
	ATTEMPTED_CALLS_RATE_OUT("AttemptedCallsRateOut", "attemptedCallsRateOut"),
	NO_ANSWER_CALLS_IN_TOTAL("NoAnswerCallsInTotal", "noAnswerCallsInTotal");

	private final String name;
	private final String kpiId;
}
