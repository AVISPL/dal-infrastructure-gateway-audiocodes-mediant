/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

/**
 * Maps device status data returned by the {@link Constant#STATUS_ENDPOINT} endpoint.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {
	String defaultGateway;
	String highAvailability;
	String ipAddress;
	String localTimeStamp;
	String macAddress;
	String mcUpgradeStatus;
	String operationalState;
	String productType;
	String protocolType;
	Boolean resetNeeded = false;
	Boolean saveNeeded = false;
	String serialNumber;
	String subnetMask;
	Integer systemUpTime = 0;
	String upgradeStatus;
	String versionID;
}
