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
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;

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

	/**
	 * End-to-end check that a property named in {@code historicalProperties} is reported as a dynamic
	 * statistic in addition to remaining an extended property, against the live device rather than a
	 * hand-built map (see {@code AudioCodesMediantHistoricalPropertiesTest} for the offline selection
	 * tests).
	 * <p>
	 * The values are asserted against the device's current data rather than merely checked for
	 * presence, since the point is to prove the real value survives the copy into the dynamic map
	 * intact. That does couple this test to the simulator's seed values - if it is reseeded, the
	 * expected numbers here need updating.
	 * <p>
	 * {@code displayPropertyGroups} is narrowed to the two groups involved so the test issues only the
	 * KPI requests it actually needs, and so that a property selected as historical is confirmed to
	 * work alongside a narrowed group selection rather than only under the {@code All} default.
	 */
	@Test
	void testGetMultipleStatistics_withHistoricalProperties() throws Exception {
		String answerSeizureRatioKey = Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + "AnswerSeizureRatio(%)";
		String activeSessionsKey = Constant.CALL_LOAD_STATISTICS_GROUP + Constant.HASH + "ActiveSessions";
		String networkEffectivenessRatioKey = Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + "NetworkEffectivenessRatio(%)";

		var historicalCommunicator = new AudioCodesMediantCommunicator();
		historicalCommunicator.setHost("localhost");
		historicalCommunicator.setPort(8083);
		historicalCommunicator.setLogin("admin");
		historicalCommunicator.setPassword("admin");
		historicalCommunicator.setDisplayPropertyGroups(
				Constant.CALL_QUALITY_STATISTICS_GROUP + "," + Constant.CALL_LOAD_STATISTICS_GROUP);
		historicalCommunicator.setHistoricalProperties(answerSeizureRatioKey + "," + activeSessionsKey);
		historicalCommunicator.init();
		try {
			var statistics = (ExtendedStatistics) historicalCommunicator.getMultipleStatistics().get(0);
			Map<String, String> staticStats = statistics.getStatistics();
			Map<String, String> dynamicStats = statistics.getDynamicStatistics();

			//	Both selected properties are reported dynamically, carrying the device's actual values.
			String answerSeizureRatio = dynamicStats.get(answerSeizureRatioKey);
			String activeSessions = dynamicStats.get(activeSessionsKey);
			Assertions.assertEquals("95", answerSeizureRatio, "Expected the device's answerSeizureRatio in the dynamic statistics");
			Assertions.assertEquals("8", activeSessions, "Expected the device's activeSessions in the dynamic statistics");
			Assertions.assertTrue(Util.isNumeric(answerSeizureRatio) && Util.isNumeric(activeSessions),
					"Only numeric values are eligible to be reported dynamically");

			//	And remain reported statically too - a selected property appears in both maps.
			Assertions.assertEquals("95", staticStats.get(answerSeizureRatioKey),
					"A property reported dynamically must also remain in the static statistics");
			Assertions.assertEquals("8", staticStats.get(activeSessionsKey),
					"A property reported dynamically must also remain in the static statistics");

			//	An unlisted property from one of the same groups is untouched.
			Assertions.assertEquals("97", staticStats.get(networkEffectivenessRatioKey),
					"An unlisted property must stay in the static statistics");
			Assertions.assertFalse(dynamicStats.containsKey(networkEffectivenessRatioKey),
					"An unlisted property must not be reported dynamically");
		} finally {
			historicalCommunicator.disconnect();
			historicalCommunicator.destroy();
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
