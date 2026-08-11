/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ratio/latency KPIs indicating whether calls are succeeding cleanly (device group
 * {@code sbc/callStats/global}), monitored under the {@code CallQualityStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallQualityStatsProperty implements KpiProperty {
	ANSWER_SEIZURE_RATIO("AnswerSeizureRatio(%)", "answerSeizureRatio", true),
	NETWORK_EFFECTIVENESS_RATIO("NetworkEffectivenessRatio(%)", "networkEffectivenessRatio", true),
	FAILED_CALLS_IN_RATIO("FailedCallsInRatio(%)", "failedCallsInRatio", true),
	FAILED_CALLS_OUT_RATIO("FailedCallsOutRatio(%)", "failedCallsOutRatio", true),
	POST_DIAL_DELAY("PostDialDelay(sec)", "postDialDelay", true);

	private final String name;
	private final String kpiId;
	/** @see KpiProperty#isGauge() */
	private final boolean gauge;
}
