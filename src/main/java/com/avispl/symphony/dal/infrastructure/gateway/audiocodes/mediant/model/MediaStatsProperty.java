/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Voice-quality and media-stream KPIs for the RTP legs the device is handling (device group
 * {@code media/mediaStats/global}), monitored under the {@code MediaStatistics} group.
 * <p>
 * Note: the device reports {@code mediaMOSIn}/{@code mediaMOSOut} in units of 0.1 - a raw value of
 * {@code 35} means a MOS of 3.5. These are currently reported exactly as received, without scaling.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum MediaStatsProperty implements KpiProperty {
	MEDIA_MOS_IN("MediaMOSIn", "mediaMOSIn"),
	MEDIA_MOS_OUT("MediaMOSOut", "mediaMOSOut"),
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
