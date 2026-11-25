/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

/**
 * Interface for property enums backed by a device KPI, identified by the device's KPI id.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public interface KpiProperty extends BaseProperty {
	String getKpiId();

	/**
	 * Converts a value reported by the device to the value displayed for the property.
	 *
	 * @param value the device value, never {@code null}
	 * @return the displayed value; {@code value} unchanged by default
	 */
	default String convertValue(String value) {
		return value;
	}
}
