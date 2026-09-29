/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the full response from the active alarms API endpoint,
 * which wraps the alarm list alongside pagination cursor metadata.
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmsResponse {
    private List<Alarms> alarms;
}
