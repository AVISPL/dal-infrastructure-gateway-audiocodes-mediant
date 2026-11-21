/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.IntervalKpiProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ethernet port KPIs (device entity {@code network/portStats/port/<index>}), reported once for every Ethernet port
 * under an {@code EthernetPortStatistics_<index>} group.
 * The {@code Total} KPIs are real-time counters accumulated since the device started; each KPI without the
 * {@code Total} suffix is the count of the same counter during the last completed 15-minute interval.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum EthernetPortStatsProperty implements IntervalKpiProperty {
	BYTES_IN_TOTAL("BytesInTotal", "bytesInTotal", false),
	BYTES_OUT_TOTAL("BytesOutTotal", "bytesOutTotal", false),
	PACKETS_IN_TOTAL("PacketsInTotal", "packetsInTotal", false),
	PACKETS_OUT_TOTAL("PacketsOutTotal", "packetsOutTotal", false),
	FIFO_ERROR_IN_TOTAL("FIFOErrorInTotal", "fifoErrorInTotal", false),
	FIFO_ERROR_OUT_TOTAL("FIFOErrorOutTotal", "fifoErrorOutTotal", false),
	DROPPED_IN_TOTAL("DroppedInTotal", "droppedInTotal", false),
	DROPPED_OUT_TOTAL("DroppedOutTotal", "droppedOutTotal", false),
	FRAME_ERROR_IN_TOTAL("FrameErrorInTotal", "frameErrorInTotal", false),
	COMPRESSED_IN_TOTAL("CompressedInTotal", "compressedInTotal", false),
	COLLISIONS_TOTAL("CollisionsTotal", "collisionsTotal", false),
	CARRIER_ERRORS_OUT_TOTAL("CarrierErrorsOutTotal", "carrierErrorsOutTotal", false),
	COMPRESSED_OUT_TOTAL("CompressedOutTotal", "compressedOutTotal", false),
	UNICAST_IN_TOTAL("UnicastInTotal", "unicastInTotal", false),
	UNICAST_OUT_TOTAL("UnicastOutTotal", "unicastOutTotal", false),
	MULTICAST_IN_TOTAL("MulticastInTotal", "multicastInTotal", false),
	MULTICAST_OUT_TOTAL("MulticastOutTotal", "multicastOutTotal", false),
	BROADCAST_IN_TOTAL("BroadcastInTotal", "broadcastInTotal", false),
	BROADCAST_OUT_TOTAL("BroadcastOutTotal", "broadcastOutTotal", false),
	FCS_ERROR_IN_TOTAL("FCSErrorInTotal", "fcsErrorInTotal", false),
	FCS_ERROR_OUT_TOTAL("FCSErrorOutTotal", "fcsErrorOutTotal", false),
	DISCARD_IN_TOTAL("DiscardInTotal", "discardInTotal", false),
	BYTES_IN("BytesIn", "bytesIn", true),
	BYTES_OUT("BytesOut", "bytesOut", true),
	PACKETS_IN("PacketsIn", "packetsIn", true),
	PACKETS_OUT("PacketsOut", "packetsOut", true),
	FIFO_ERROR_IN("FIFOErrorIn", "fifoErrorIn", true),
	FIFO_ERROR_OUT("FIFOErrorOut", "fifoErrorOut", true),
	DROPPED_IN("DroppedIn", "droppedIn", true),
	DROPPED_OUT("DroppedOut", "droppedOut", true),
	FRAME_ERROR_IN("FrameErrorIn", "frameErrorIn", true),
	COMPRESSED_IN("CompressedIn", "compressedIn", true),
	COLLISIONS("Collisions", "collisions", true),
	CARRIER_ERRORS_OUT("CarrierErrorsOut", "carrierErrorsOut", true),
	COMPRESSED_OUT("CompressedOut", "compressedOut", true),
	UNICAST_IN("UnicastIn", "unicastIn", true),
	UNICAST_OUT("UnicastOut", "unicastOut", true),
	MULTICAST_IN("MulticastIn", "multicastIn", true),
	MULTICAST_OUT("MulticastOut", "multicastOut", true),
	BROADCAST_IN("BroadcastIn", "broadcastIn", true),
	BROADCAST_OUT("BroadcastOut", "broadcastOut", true),
	FCS_ERROR_IN("FCSErrorIn", "fcsErrorIn", true),
	FCS_ERROR_OUT("FCSErrorOut", "fcsErrorOut", true),
	DISCARD_IN("DiscardIn", "discardIn", true);

	private final String name;
	private final String kpiId;
	private final boolean lastInterval;
}
