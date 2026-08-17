/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SIP-REC (session recording) volume and establishment-rate KPIs (device group
 * {@code sbc/sipRecStats/global}), monitored under the {@code SipRecStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SipRecStatsProperty implements KpiProperty {
	SIP_REC_SESSIONS("SIPRecSessions", "sipRecSessions"),
	SIP_REC_RATE("SIPRecRate(sps)", "sipRecRate");

	private final String name;
	private final String kpiId;
}
