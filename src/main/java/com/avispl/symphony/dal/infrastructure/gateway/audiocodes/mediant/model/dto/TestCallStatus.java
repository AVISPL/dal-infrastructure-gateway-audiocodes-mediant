/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the response of a
 * {@code GET /sipTestCall/getStatus?sessionId=<id>} request.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCallStatus {
	private String callStatus;
	private String callId;
	/** Only populated by the device once {@code callStatus} is {@code Disconnected} or {@code Failed,NoResources}. */
	private String releaseCause;
}
