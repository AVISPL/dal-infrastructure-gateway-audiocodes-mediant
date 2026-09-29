/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

/**
 * Marker interface for property enums backed by a device KPI, fetched individually via
 * {@code /kpi/current/sbc/callStats/global/<kpiId>}.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public interface KpiProperty extends BaseProperty {
	String getKpiId();
}
