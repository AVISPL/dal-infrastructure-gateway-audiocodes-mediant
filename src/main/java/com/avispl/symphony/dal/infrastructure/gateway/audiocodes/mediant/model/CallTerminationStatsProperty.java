/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * KPIs for calls that broke/dropped unexpectedly (device group {@code sbc/callStats/global}),
 * monitored under the {@code CallTerminationStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallTerminationStatsProperty implements KpiProperty {
	ABNORMAL_TERMINATED_CALLS_IN_TOTAL("AbnormalTerminatedCallsInTotal", "abnormalTerminatedCallsInTotal"),
	ABNORMAL_TERMINATED_CALLS_OUT_TOTAL("AbnormalTerminatedCallsOutTotal", "abnormalTerminatedCallsOutTotal"),
	MEDIA_BROKEN_CONNECTION_CALLS("MediaBrokenConnectionCalls", "mediaBrokenConnectionCalls");

	private final String name;
	private final String kpiId;
}
