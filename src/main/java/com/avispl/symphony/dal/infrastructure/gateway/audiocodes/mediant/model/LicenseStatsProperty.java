/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * License key usage KPIs (device group {@code system/licenseStats/global}), monitored under the
 * {@code LicenseStatistics} group. Each value is the percentage of the licensed capacity in use.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum LicenseStatsProperty implements KpiProperty {
	LICENSE_SBC_SIGNALING_USAGE("LicenseSBCSignalingUsage(%)", "licenseSbcSignalingUsage"),
	LICENSE_SBC_MEDIA_USAGE("LicenseSBCMediaUsage(%)", "licenseSbcMediaUsage"),
	LICENSE_TRANSCODING_USAGE("LicenseTranscodingUsage(%)", "licenseTranscodingUsage"),
	LICENSE_SIP_REC_USAGE("LicenseSIPRecUsage(%)", "licenseSipRecUsage"),
	LICENSE_WEB_RTC_USAGE("LicenseWebRTCUsage(%)", "licenseWebRTCUsage"),
	LICENSE_FEU_USAGE("LicenseFEUUsage(%)", "licenseFeuUsage");

	private final String name;
	private final String kpiId;
}
