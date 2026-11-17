/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SIP TLS connection KPIs (device entity {@code network/tlsStats/global}), reported under the {@code SIPTLSStatistics}
 * group.
 * The {@code Total} KPIs are counters accumulated since the device started; the attempted and rejected connection KPIs
 * without the {@code Total} suffix are the counts during the last completed 15-minute interval, and the average and
 * maximum active connection KPIs are the average and maximum of that interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SipTlsStatsProperty implements IntervalKpiProperty {
	SIP_TLS_ACTIVE_CONNECTIONS("SIPTLSActiveConnections", "activeSipTlsConn", false),
	SIP_TLS_AVERAGE_ACTIVE_CONNECTIONS("SIPTLSAverageActiveConnections", "activeSipTlsConnAvg", true),
	SIP_TLS_MAXIMUM_ACTIVE_CONNECTIONS("SIPTLSMaximumActiveConnections", "activeSipTlsConnMax", true),
	SIP_TLS_ATTEMPTED_CONNECTIONS_TOTAL("SIPTLSAttemptedConnectionsTotal", "attemptedSipTlsConnTotal", false),
	SIP_TLS_ATTEMPTED_CONNECTIONS("SIPTLSAttemptedConnections", "attemptedSipTlsConn", true),
	SIP_TLS_REJECTED_CONNECTIONS_TOTAL("SIPTLSRejectedConnectionsTotal", "rejectedSipTlsConnTotal", false),
	SIP_TLS_REJECTED_CONNECTIONS("SIPTLSRejectedConnections", "rejectedSipTlsConn", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;
}
