/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One entry of a KPI node listing, such as an IP Group entity
 * ({@code /kpi/current/sbc/callStats/ipGroup}) or a KPI value of one IP Group
 * ({@code /kpi/current/sbc/callStats/ipGroup/<index>}).
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KpiItem {
	/** Node id: the entity index for an IP Group entry, or the KPI id for a KPI value entry. */
	private String id;
	private String name;
	private String description;
	private String url;
	/** KPI value; absent for entity entries. */
	private String value;
}
