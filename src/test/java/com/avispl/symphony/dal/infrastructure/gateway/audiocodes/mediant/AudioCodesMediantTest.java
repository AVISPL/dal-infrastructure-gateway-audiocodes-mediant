/**
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

/**
 * AudioCodesMediantTest class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantTest {
	private AudioCodesMediantCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new AudioCodesMediantCommunicator();
		this.communicator.setHost("localhost");
		this.communicator.setPort(8083);
		this.communicator.setLogin("admin");
		this.communicator.setPassword("admin");
		this.communicator.setDisplayPropertyGroups(Constant.CALL_STATS_ALL_GROUPS);
		this.communicator.init();
	}

	@AfterEach
	void destroy() throws Exception {
		this.communicator.disconnect();
		this.communicator.destroy();
	}

	@Test
	void testLogin_withInvalidCredential() throws Exception {
		var invalidCredsCommunicator = new AudioCodesMediantCommunicator();
		invalidCredsCommunicator.setHost("localhost");
		invalidCredsCommunicator.setPort(8083);
		invalidCredsCommunicator.setLogin("admin");
		invalidCredsCommunicator.setPassword("wrong-password");
		invalidCredsCommunicator.init();
		try {
			Assertions.assertThrows(FailedLoginException.class, invalidCredsCommunicator::getMultipleStatistics);
		} finally {
			invalidCredsCommunicator.disconnect();
			invalidCredsCommunicator.destroy();
		}
	}

	@Test
	void testGetMultipleStatistics_withGeneral() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var generalProperties = this.filterGroupStatistics(statistics.getStatistics(), null);

		Assertions.assertTrue(MapUtils.isNotEmpty(generalProperties));
		generalProperties.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
	}

	@Test
	void testGetMultipleStatistics_withNetworkGroup() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var networkGroup = this.filterGroupStatistics(statistics.getStatistics(), Constant.NETWORK_GROUP);

		Assertions.assertTrue(MapUtils.isNotEmpty(networkGroup));
		networkGroup.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
	}

	@Test
	void testGetMultipleStatistics_withCallStatsGroups() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		List<String> kpiGroups = List.of(
				Constant.CALL_LOAD_STATISTICS_GROUP,
				Constant.CALL_QUALITY_STATISTICS_GROUP,
				Constant.CALL_TERMINATION_STATISTICS_GROUP,
				Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP,
				Constant.CALL_CAPACITY_STATISTICS_GROUP,
				Constant.CALL_ROUTING_STATISTICS_GROUP,
				Constant.CALL_TRAFFIC_STATISTICS_GROUP,
				Constant.MEDIA_STATISTICS_GROUP,
				Constant.MEDIA_DSP_STATISTICS_GROUP,
				Constant.MEDIA_CLUSTER_STATISTICS_GROUP,
				Constant.REGISTRATION_STATISTICS_GROUP,
				Constant.SIP_REC_STATISTICS_GROUP);

		for (String group : kpiGroups) {
			var groupStats = this.filterGroupStatistics(statistics.getStatistics(), group);
			Assertions.assertTrue(MapUtils.isNotEmpty(groupStats), "Expected non-empty stats for group " + group);
			groupStats.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
		}

		//	The assertions above only check that each group is non-empty, so a typo in a property display name
		//	would go unnoticed. These names carry uppercase acronyms per the Adapter Extended Properties Naming
		//	Guidelines and are spelled out literally on purpose - reading them from the enum would make the test
		//	follow any rename instead of catching it.
		List<String> acronymPropertyKeys = List.of(
				Constant.MEDIA_CLUSTER_STATISTICS_GROUP + Constant.HASH + "DSPClusterUtilization(%)",
				Constant.MEDIA_DSP_STATISTICS_GROUP + Constant.HASH + "DSPResourceCurrent(%)",
				Constant.MEDIA_DSP_STATISTICS_GROUP + Constant.HASH + "SBCSessionsCoderTranscoding",
				Constant.MEDIA_DSP_STATISTICS_GROUP + Constant.HASH + "SBCSessionsCoderTranscoding(%)",
				Constant.REGISTRATION_STATISTICS_GROUP + Constant.HASH + "SBCRegistrationSuccessRatio(%)",
				Constant.SIP_REC_STATISTICS_GROUP + Constant.HASH + "SIPRecSessions",
				Constant.SIP_REC_STATISTICS_GROUP + Constant.HASH + "SIPRecRate(sps)",
				Constant.MEDIA_STATISTICS_GROUP + Constant.HASH + "MediaRTPStreams");

		for (String key : acronymPropertyKeys) {
			Assertions.assertTrue(statistics.getStatistics().containsKey(key),
					"Expected property " + key + " to be present; check the display name in the corresponding KpiProperty enum");
		}
	}

	@Test
	void testControlProperty_startAndStopCallDiagnostic() throws Exception {
		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
		String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
		String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

		var beforeDial = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		Assertions.assertEquals(Constant.CALL_DIAGNOSTICS_NOT_DIALED, beforeDial.getStatistics().get(statusKey));
		Assertions.assertFalse(beforeDial.getStatistics().containsKey(stopKey), "Stop should not be present before the first dial");

		this.communicator.controlProperty(new ControllableProperty(calledNumberKey, "200", null));
		this.communicator.controlProperty(new ControllableProperty(callingNumberKey, "100", null));
		this.communicator.controlProperty(new ControllableProperty(destinationKey, "10.4.219.229", null));
		this.communicator.controlProperty(new ControllableProperty(startKey, "1", null));

		var afterStart = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		String statusAfterStart = afterStart.getStatistics().get(statusKey);
		Assertions.assertTrue(this.isValidValue(statusAfterStart));
		Assertions.assertNotEquals(Constant.CALL_DIAGNOSTICS_NOT_DIALED, statusAfterStart);
		//	The test call may already have disconnected by this second poll (it's short-lived on this device/simulator),
		//	so assert Stop's presence tracks the actual status rather than assuming it's still non-disconnected.
		boolean expectStopPresent = !Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(statusAfterStart);
		Assertions.assertEquals(expectStopPresent, afterStart.getStatistics().containsKey(stopKey),
				"Stop presence should match whether the call is disconnected (status: %s)".formatted(statusAfterStart));

		this.communicator.controlProperty(new ControllableProperty(stopKey, "1", null));

		var afterStop = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		Assertions.assertEquals(Constant.CALL_DIAGNOSTICS_DISCONNECTED, afterStop.getStatistics().get(statusKey));
		Assertions.assertFalse(afterStop.getStatistics().containsKey(stopKey), "Stop should be removed once the call is disconnected");
	}

	@Test
	void testGetMultipleStatistics_withCallDiagnosticsGroupDisabled() throws Exception {
		var noDiagnosticsCommunicator = new AudioCodesMediantCommunicator();
		noDiagnosticsCommunicator.setHost("localhost");
		noDiagnosticsCommunicator.setPort(8083);
		noDiagnosticsCommunicator.setLogin("admin");
		noDiagnosticsCommunicator.setPassword("admin");
		noDiagnosticsCommunicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		noDiagnosticsCommunicator.init();
		try {
			var statistics = (ExtendedStatistics) noDiagnosticsCommunicator.getMultipleStatistics().get(0);
			var diagnosticsGroup = this.filterGroupStatistics(statistics.getStatistics(), Constant.CALL_DIAGNOSTICS_GROUP);
			Assertions.assertTrue(MapUtils.isEmpty(diagnosticsGroup), "CallDiagnostics should be absent when not listed in displayPropertyGroups");
		} finally {
			noDiagnosticsCommunicator.disconnect();
			noDiagnosticsCommunicator.destroy();
		}
	}

	/**
	 * Each new KPI group must be selectable on its own. This is the only test that catches a group name
	 * missing from {@link Constant#SUPPORTED_PROPERTY_GROUPS}: an unsupported name is filtered out of
	 * {@code displayPropertyGroups}, which leaves the list empty and falls back to
	 * {@link Constant#CALL_STATS_ALL_GROUPS} - enabling every group. Asserting the selected group is
	 * present would therefore pass either way, so the load-bearing assertion is that a group which was
	 * <em>not</em> selected is absent.
	 */
	@Test
	void testGetMultipleStatistics_withSingleGroupEnabled() throws Exception {
		List<String> newKpiGroups = List.of(
				Constant.MEDIA_STATISTICS_GROUP,
				Constant.MEDIA_DSP_STATISTICS_GROUP,
				Constant.MEDIA_CLUSTER_STATISTICS_GROUP,
				Constant.REGISTRATION_STATISTICS_GROUP,
				Constant.SIP_REC_STATISTICS_GROUP);

		for (String group : newKpiGroups) {
			var singleGroupCommunicator = new AudioCodesMediantCommunicator();
			singleGroupCommunicator.setHost("localhost");
			singleGroupCommunicator.setPort(8083);
			singleGroupCommunicator.setLogin("admin");
			singleGroupCommunicator.setPassword("admin");
			singleGroupCommunicator.setDisplayPropertyGroups(group);
			singleGroupCommunicator.init();
			try {
				var statistics = (ExtendedStatistics) singleGroupCommunicator.getMultipleStatistics().get(0);
				Assertions.assertTrue(MapUtils.isNotEmpty(this.filterGroupStatistics(statistics.getStatistics(), group)),
						"Expected non-empty stats for group " + group + " when it is the only group enabled");
				Assertions.assertTrue(MapUtils.isEmpty(this.filterGroupStatistics(statistics.getStatistics(), Constant.CALL_LOAD_STATISTICS_GROUP)),
						"CallLoadStatistics should be absent when only " + group + " is enabled; if it is present, "
								+ group + " is likely missing from SUPPORTED_PROPERTY_GROUPS and the 'All' fallback kicked in");
			} finally {
				singleGroupCommunicator.disconnect();
				singleGroupCommunicator.destroy();
			}
		}
	}

	private Map<String, String> filterGroupStatistics(Map<String, String> statistics, String groupName) {
		return statistics.entrySet().stream()
				.filter(e -> groupName == null ? !e.getKey().contains(Constant.HASH) : e.getKey().startsWith(groupName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty() && !value.isBlank();
	}
}
