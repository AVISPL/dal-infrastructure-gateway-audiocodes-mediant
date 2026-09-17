/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an individual alarm instance
 * fetched from the AudioCodes Mediant device.
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarms {
    private int id;
    private String description;
    private String severity;
    private String source;
    private String date;
    private String url;
}
