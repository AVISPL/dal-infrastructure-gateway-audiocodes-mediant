/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;

/**
 * Enum representing General section property names mapped from {@link Status} data.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum GeneralProperty {
	HIGH_AVAILABILITY("HighAvailability"),
	LOCAL_TIMESTAMP("LocalTimestamp"),
	MC_UPGRADE_STATUS("MCUpgradeStatus"),
	OPERATIONAL_STATE("OperationalState"),
	PRODUCT_TYPE("ProductType"),
	PROTOCOL_TYPE("ProtocolType"),
	VERSION_ID("VersionID"),
	SAVE_NEEDED("SaveNeeded"),
	SERIAL_NUMBER("SerialNumber"),
	SYSTEM_UPTIME("SystemUptime(sec)"),
	UPGRADE_STATUS("UpgradeStatus"),
	RESET_NEEDED("ResetNeeded"),
	;

	String displayName;

	/**
	 * Extracts the string value of a specific {@link GeneralProperty} from the given {@link Status}.
	 *
	 * <p>Acts as a centralized dispatcher: each enum constant maps to its corresponding
	 * getter on {@code deviceStatus}, with non-{@code String} return types coerced via
	 * {@code toString()}.
	 *
	 * @param property the property to retrieve; must not be {@code null}
	 * @param deviceStatus the device status object to extract the value from; must not be {@code null}
	 * @return the string representation of the requested property; never {@code null}
	 * unless the underlying getter itself returns {@code null}
	 * @throws NullPointerException if {@code property} or {@code deviceStatus} is {@code null}
	 */
	public static String getPropertyValue(GeneralProperty property, Status deviceStatus) {
		return switch (property) {
			case HIGH_AVAILABILITY -> deviceStatus.getHighAvailability();
			case LOCAL_TIMESTAMP -> deviceStatus.getLocalTimeStamp();
			case MC_UPGRADE_STATUS -> deviceStatus.getMcUpgradeStatus();
			case OPERATIONAL_STATE -> deviceStatus.getOperationalState();
			case PRODUCT_TYPE -> deviceStatus.getProductType();
			case PROTOCOL_TYPE -> deviceStatus.getProtocolType();
			case VERSION_ID -> deviceStatus.getVersionID();
			case SAVE_NEEDED -> deviceStatus.getSaveNeeded().toString();
			case SERIAL_NUMBER -> deviceStatus.getSerialNumber();
			case SYSTEM_UPTIME -> deviceStatus.getSystemUpTime().toString();
			case UPGRADE_STATUS -> deviceStatus.getUpgradeStatus();
			case RESET_NEEDED -> deviceStatus.getResetNeeded().toString();
		};
	}
}
