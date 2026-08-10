/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * KPIs for calls the SBC itself rejected due to resource/policy limits, not network trouble
 * (device group {@code sbc/callStats/global}), monitored under the {@code CallCapacityStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CallCapacityStatsProperty implements KpiProperty {
	NO_RESOURCES_CALLS_IN("NoResourcesCallsIn", "noResourcesCallsIn"),
	NO_RESOURCES_CALLS_OUT("NoResourcesCallsOut", "noResourcesCallsOut"),
	ADMISSION_FAILED_CALLS_IN("AdmissionFailedCallsIn", "admissionFailedCallsIn"),
	ADMISSION_FAILED_CALLS_OUT("AdmissionFailedCallsOut", "admissionFailedCallsOut");

	private final String name;
	private final String kpiId;
}
