/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Codec/negotiation-level media failure KPIs (device group {@code sbc/callStats/global}),
 * monitored under the {@code CallMediaIssuesStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallMediaIssuesStatsProperty implements KpiProperty {
	MEDIA_MISMATCH_CALLS_IN("MediaMismatchCallsIn", "mediaMismatchCallsIn"),
	MEDIA_MISMATCH_CALLS_OUT("MediaMismatchCallsOut", "mediaMismatchCallsOut");

	private final String name;
	private final String kpiId;
}
