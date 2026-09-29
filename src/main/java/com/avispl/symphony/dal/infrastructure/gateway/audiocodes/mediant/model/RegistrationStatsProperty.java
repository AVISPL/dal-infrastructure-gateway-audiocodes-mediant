/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SIP registration volume, throughput and success-rate KPIs (device group
 * {@code sbc/otherStats/global}), monitored under the {@code RegistrationStatistics} group.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum RegistrationStatsProperty implements KpiProperty {
	REGISTERED_USERS("RegisteredUsers", "registeredUsers"),
	TRANSACTION_RATE("TransactionRate(tps)", "transactionRate"),
	REGISTER_RATE_IN("RegisterRateIn(rps)", "registerRateIn"),
	REGISTER_RATE_OUT("RegisterRateOut(rps)", "registerRateOut"),
	SBC_REGISTRATION_SUCCESS_RATIO("SBCRegistrationSuccessRatio(%)", "sbcRegistrationSuccessRatio"),
	USER_REGISTRATION_SUCCESS_RATIO("UserRegistrationSuccessRatio(%)", "userRegistrationSuccessRatio");

	private final String name;
	private final String kpiId;
}
