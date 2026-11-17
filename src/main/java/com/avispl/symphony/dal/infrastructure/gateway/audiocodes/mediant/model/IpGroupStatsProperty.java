/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Per-IP-Group KPIs, reported once for every IP Group under an {@code IPGroupStatistics_<index>_<name>} group.
 * Each KPI is read from the IP Group entity of the device scope in {@link #getScopeApi()}
 * (e.g. {@code /kpi/current/sbc/callStats/ipGroup/<index>}).
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum IpGroupStatsProperty implements KpiProperty {
	ACTIVE_CALLS_IN("ActiveCallsIn", "activeCallsIn", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ACTIVE_CALLS_OUT("ActiveCallsOut", "activeCallsOut", Constant.IP_GROUP_CALL_STATS_KPI_API),
	AVERAGE_CALL_DURATION("AverageCallDuration(sec)", "averageCallDuration", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ATTEMPTED_CALLS_IN_TOTAL("AttemptedCallsInTotal", "attemptedCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ATTEMPTED_CALLS_OUT_TOTAL("AttemptedCallsOutTotal", "attemptedCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ESTABLISHED_CALLS_IN_TOTAL("EstablishedCallsInTotal", "establishedCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ESTABLISHED_CALLS_OUT_TOTAL("EstablishedCallsOutTotal", "establishedCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ATTEMPTED_CALLS_RATE_IN("AttemptedCallsRateIn(cps)", "attemptedCallsRateIn", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ATTEMPTED_CALLS_RATE_OUT("AttemptedCallsRateOut(cps)", "attemptedCallsRateOut", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ANSWER_SEIZURE_RATIO("AnswerSeizureRatio(%)", "answerSeizureRatio", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NETWORK_EFFECTIVENESS_RATIO("NetworkEffectivenessRatio(%)", "networkEffectivenessRatio", Constant.IP_GROUP_CALL_STATS_KPI_API),
	FAILED_CALLS_IN_RATIO("FailedCallsInRatio(%)", "failedCallsInRatio", Constant.IP_GROUP_CALL_STATS_KPI_API),
	FAILED_CALLS_OUT_RATIO("FailedCallsOutRatio(%)", "failedCallsOutRatio", Constant.IP_GROUP_CALL_STATS_KPI_API),
	POST_DIAL_DELAY("PostDialDelay(sec)", "postDialDelay", Constant.IP_GROUP_CALL_STATS_KPI_API),
	BUSY_CALLS_IN_TOTAL("BusyCallsInTotal", "busyCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	BUSY_CALLS_OUT_TOTAL("BusyCallsOutTotal", "busyCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NO_ANSWER_CALLS_IN_TOTAL("NoAnswerCallsInTotal", "noAnswerCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NO_ANSWER_CALLS_OUT_TOTAL("NoAnswerCallsOutTotal", "noAnswerCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NO_RESOURCES_CALLS_IN_TOTAL("NoResourcesCallsInTotal", "noResourcesCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NO_RESOURCES_CALLS_OUT_TOTAL("NoResourcesCallsOutTotal", "noResourcesCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ADMISSION_FAILED_CALLS_IN_TOTAL("AdmissionFailedCallsInTotal", "admissionFailedCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ADMISSION_FAILED_CALLS_OUT_TOTAL("AdmissionFailedCallsOutTotal", "admissionFailedCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	MEDIA_MISMATCH_CALLS_IN_TOTAL("MediaMismatchCallsInTotal", "mediaMismatchCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	MEDIA_MISMATCH_CALLS_OUT_TOTAL("MediaMismatchCallsOutTotal", "mediaMismatchCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	NO_ROUTE_CALLS_IN_TOTAL("NoRouteCallsInTotal", "noRouteCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ABNORMAL_TERMINATED_CALLS_IN_TOTAL("AbnormalTerminatedCallsInTotal", "abnormalTerminatedCallsInTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	ABNORMAL_TERMINATED_CALLS_OUT_TOTAL("AbnormalTerminatedCallsOutTotal", "abnormalTerminatedCallsOutTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	MEDIA_BROKEN_CONNECTION_CALLS_TOTAL("MediaBrokenConnectionCallsTotal", "mediaBrokenConnectionCallsTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	MINUTES_OF_USAGE_TOTAL("MinutesOfUsageTotal(min)", "minutesOfUsageTotal", Constant.IP_GROUP_CALL_STATS_KPI_API),
	MEDIA_MOS_IN("MediaMOSIn", "mediaMOSIn", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_MOS_OUT("MediaMOSOut", "mediaMOSOut", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_JITTER_IN("MediaJitterIn(ms)", "mediaJitterIn", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_JITTER_OUT("MediaJitterOut(ms)", "mediaJitterOut", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_DELAY_IN("MediaDelayIn(ms)", "mediaDelayIn", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_DELAY_OUT("MediaDelayOut(ms)", "mediaDelayOut", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_PACKET_LOSS_IN("MediaPacketLossIn(%)", "mediaPacketLossIn", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_PACKET_LOSS_OUT("MediaPacketLossOut(%)", "mediaPacketLossOut", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_BANDWIDTH_IN("MediaBandwidthIn(Kbps)", "mediaBandwidthIn", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_BANDWIDTH_OUT("MediaBandwidthOut(Kbps)", "mediaBandwidthOut", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_STREAMS("MediaStreams", "mediaStreams", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	MEDIA_RTP_STREAMS("MediaRTPStreams", "mediaRtpStreams", Constant.IP_GROUP_MEDIA_STATS_KPI_API),
	REGISTERED_USERS("RegisteredUsers", "registeredUsers", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	TRANSACTION_RATE("TransactionRate(tps)", "transactionRate", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	REGISTER_RATE_IN("RegisterRateIn(rps)", "registerRateIn", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	REGISTER_RATE_OUT("RegisterRateOut(rps)", "registerRateOut", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	SBC_REGISTRATION_SUCCESS_RATIO("SBCRegistrationSuccessRatio(%)", "sbcRegistrationSuccessRatio", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	USER_REGISTRATION_SUCCESS_RATIO("UserRegistrationSuccessRatio(%)", "userRegistrationSuccessRatio", Constant.IP_GROUP_OTHER_STATS_KPI_API),
	SIP_REC_SESSIONS("SIPRecSessions", "sipRecSessions", Constant.IP_GROUP_SIP_REC_STATS_KPI_API),
	SIP_REC_RATE("SIPRecRate(sps)", "sipRecRate", Constant.IP_GROUP_SIP_REC_STATS_KPI_API);

	private final String name;
	private final String kpiId;
	private final String scopeApi;
}
