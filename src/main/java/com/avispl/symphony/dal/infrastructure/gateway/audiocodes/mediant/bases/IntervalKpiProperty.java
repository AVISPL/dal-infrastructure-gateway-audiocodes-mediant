/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

/**
 * Interface for property enums backed by a device KPI that is either a real-time value
 * ({@code /kpi/current/...}) or the value of the last completed 15-minute interval ({@code /kpi/history/...}).
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public interface IntervalKpiProperty extends KpiProperty {
	/**
	 * @return {@code true} if the KPI is read for the last completed interval from the history endpoint;
	 * {@code false} if it is a real-time value
	 */
	boolean isLastInterval();
}
