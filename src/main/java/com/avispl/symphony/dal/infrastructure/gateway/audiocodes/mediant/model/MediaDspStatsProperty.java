/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DSP resource consumption and transcoding load KPIs (device group {@code media/dspStats/global}),
 * monitored under the {@code MediaDSPStatistics} group.
 * <p>
 * {@code SBCSessionsCoderTranscoding} and {@code SBCSessionsCoderTranscoding(%)} are deliberately
 * near-identical names: the device reports transcoding sessions both as an absolute count and as a
 * percentage of capacity.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MediaDspStatsProperty implements KpiProperty {
	DSP_RESOURCE_CURRENT("DSPResourceCurrent(%)", "dspResourceCurrentPercent"),
	SBC_SESSIONS_CODER_TRANSCODING("SBCSessionsCoderTranscoding", "sbcSessionsCoderTranscoding"),
	SBC_SESSIONS_CODER_TRANSCODING_PERCENT("SBCSessionsCoderTranscoding(%)", "sbcSessionsCoderTranscodingPercent");

	private final String name;
	private final String kpiId;
}
