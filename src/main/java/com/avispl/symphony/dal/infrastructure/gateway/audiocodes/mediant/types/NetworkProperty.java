/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;

/**
 * Enum representing Network section property names mapped from {@link Status} data.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum NetworkProperty {
	DEFAULT_GATEWAY("DefaultGateway"),
	IP_ADDRESS("IPAddress"),
	MAC_ADDRESS("MACAddress"),
	SUBNET_MASK("SubnetMask");

	String displayName;

	public String getDisplayName() {
		return Constant.NETWORK_GROUP + Constant.HASH + displayName;
	}

	/**
	 * Extracts the string value of a specific {@link NetworkProperty} from the given {@link Status}.
	 *
	 * <p>Acts as a centralized dispatcher: each enum constant maps directly to its
	 * corresponding getter on {@code deviceStatus}.
	 *
	 * @param property the network property to retrieve; must not be {@code null}
	 * @param deviceStatus the device status object to extract the value from; must not be {@code null}
	 * @return the string representation of the requested property; never {@code null}
	 * unless the underlying getter itself returns {@code null}
	 * @throws NullPointerException if {@code property} or {@code deviceStatus} is {@code null}
	 */
	public static String getPropertyValue(NetworkProperty property, Status deviceStatus) {
		return switch (property) {
			case DEFAULT_GATEWAY -> deviceStatus.getDefaultGateway();
			case IP_ADDRESS -> deviceStatus.getIpAddress();
			case MAC_ADDRESS -> deviceStatus.getMacAddress();
			case SUBNET_MASK -> deviceStatus.getSubnetMask();
		};
	}
}
