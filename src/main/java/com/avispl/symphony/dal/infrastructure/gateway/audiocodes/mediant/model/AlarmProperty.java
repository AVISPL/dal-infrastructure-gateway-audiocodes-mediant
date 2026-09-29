/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.BaseProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents alarm configuration and monitoring properties.
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum AlarmProperty implements BaseProperty {
    DESCRIPTION("Description"),
    SEVERITY("Severity"),
    SOURCE("Source"),
    DATE("Date");
    private final String name;
}
