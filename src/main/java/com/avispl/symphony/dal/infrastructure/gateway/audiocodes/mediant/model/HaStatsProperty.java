/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * High Availability link KPIs (device entity {@code network/haStats/global}): the percentage of HA keepalive packets
 * lost by the Active and the Redundant unit, reported under the {@code HAStatistics} group.
 * The device reports these KPIs in units of 0.01%; the properties report them in percent with two decimal places.
 * The {@code Avg} and {@code Max} KPIs are the average and maximum of the last completed 15-minute interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum HaStatsProperty implements IntervalKpiProperty {
	HA_ACTIVE_PACKET_LOSS("HAActivePacketLoss(%)", "haActivePacketLoss", false),
	HA_AVERAGE_ACTIVE_PACKET_LOSS("HAAverageActivePacketLoss(%)", "haActivePacketLossAvg", true),
	HA_MAXIMUM_ACTIVE_PACKET_LOSS("HAMaximumActivePacketLoss(%)", "haActivePacketLossMax", true),
	HA_REDUNDANT_PACKET_LOSS("HARedundantPacketLoss(%)", "haRedundantPacketLoss", false),
	HA_AVERAGE_REDUNDANT_PACKET_LOSS("HAAverageRedundantPacketLoss(%)", "haRedundantPacketLossAvg", true),
	HA_MAXIMUM_REDUNDANT_PACKET_LOSS("HAMaximumRedundantPacketLoss(%)", "haRedundantPacketLossMax", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;

	/**
	 * Converts a device value in units of 0.01% to percent with two decimal places, e.g. {@code 15} to {@code 0.15}.
	 *
	 * @param value the device value
	 * @return the value in percent; {@code value} unchanged if it is not a number
	 */
	@Override
	public String convertValue(String value) {
		try {
			return new BigDecimal(value.strip()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		} catch (NumberFormatException e) {
			return value;
		}
	}
}
