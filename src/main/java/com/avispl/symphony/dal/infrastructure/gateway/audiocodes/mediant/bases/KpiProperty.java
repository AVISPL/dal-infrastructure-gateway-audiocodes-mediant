/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

/**
 * Marker interface for property enums backed by a device KPI. The whole
 * {@code /kpi/current/sbc/callStats/global} scope is fetched in a single request per poll cycle and
 * each property is resolved by looking up its {@link #getKpiId()} in that response.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public interface KpiProperty extends BaseProperty {
	String getKpiId();

	/**
	 * Whether this KPI is a gauge - a point-in-time measurement that is meaningful as a trend line -
	 * rather than a cumulative counter. Counters reset to zero at their maximum or on device reset, so
	 * plotting them yields sawtooth artefacts instead of usable history. Only gauges are reported as
	 * dynamic statistics; everything else is reported as a regular statistic.
	 *
	 * @return {@code true} if this KPI should be routed into the dynamic statistics map
	 */
	default boolean isGauge() {
		return false;
	}
}
