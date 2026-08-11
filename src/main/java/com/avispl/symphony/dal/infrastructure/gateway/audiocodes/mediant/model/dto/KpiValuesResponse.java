/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the full response from the device's scope-level performance-monitoring (KPI) endpoint
 * (e.g. {@code /kpi/current/sbc/callStats/global}), which returns every KPI belonging to that scope
 * in a single {@code items} array.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpiValuesResponse {
    private List<KpiValue> items;
}
