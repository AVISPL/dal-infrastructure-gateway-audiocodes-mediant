/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Voice-quality and media-stream KPIs for the RTP legs the device is handling (device group
 * {@code media/mediaStats/global}), monitored under the {@code MediaStatistics} group.
 * <p>
 * The device reports {@code mediaMOSIn}/{@code mediaMOSOut} in units of 0.1 on a scale of 10-50; the
 * {@code MediaMOSIn}/{@code MediaMOSOut} properties report the MOS with one decimal place (a device value of
 * {@code 35} is reported as {@code 3.5}).
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MediaStatsProperty implements KpiProperty {
	MEDIA_MOS_IN("MediaMOSIn", "mediaMOSIn") {
		/**
		 * Converts a device value in units of 0.1 to the MOS with one decimal place, e.g. {@code 42} to {@code 4.2}.
		 *
		 * @param value the device value
		 * @return the MOS; {@code value} unchanged if it is not a number
		 */
		@Override
		public String convertValue(String value) {
			return Util.divide(value, 10, 1);
		}
	},
	MEDIA_MOS_OUT("MediaMOSOut", "mediaMOSOut") {
		/**
		 * Converts a device value in units of 0.1 to the MOS with one decimal place, e.g. {@code 42} to {@code 4.2}.
		 *
		 * @param value the device value
		 * @return the MOS; {@code value} unchanged if it is not a number
		 */
		@Override
		public String convertValue(String value) {
			return Util.divide(value, 10, 1);
		}
	},
	MEDIA_JITTER_IN("MediaJitterIn(ms)", "mediaJitterIn"),
	MEDIA_JITTER_OUT("MediaJitterOut(ms)", "mediaJitterOut"),
	MEDIA_DELAY_IN("MediaDelayIn(ms)", "mediaDelayIn"),
	MEDIA_DELAY_OUT("MediaDelayOut(ms)", "mediaDelayOut"),
	MEDIA_PACKET_LOSS_IN("MediaPacketLossIn(%)", "mediaPacketLossIn"),
	MEDIA_PACKET_LOSS_OUT("MediaPacketLossOut(%)", "mediaPacketLossOut"),
	MEDIA_BANDWIDTH_IN("MediaBandwidthIn(Kbps)", "mediaBandwidthIn"),
	MEDIA_BANDWIDTH_OUT("MediaBandwidthOut(Kbps)", "mediaBandwidthOut"),
	MEDIA_STREAMS("MediaStreams", "mediaStreams"),
	MEDIA_RTP_STREAMS("MediaRTPStreams", "mediaRtpStreams");

	private final String name;
	private final String kpiId;
}
