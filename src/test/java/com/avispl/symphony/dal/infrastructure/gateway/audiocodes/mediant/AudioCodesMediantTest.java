/**
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

	/**
	 * A staged {@code Destination} is stored stripped: the dial payload forwards these values verbatim,
	 * so surrounding whitespace on a pasted address would otherwise reach the device as-is.
	 */
	@Test
	void testControlProperty_destinationIsStoredStripped() throws Exception {
		String stored = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_DESTINATION, "10.4.219.229 ");
		Assertions.assertEquals("10.4.219.229", stored, "A trailing space must not reach the stored Destination");
	}

	@Test
	void testControlProperty_calledAndCallingNumberAreStoredStripped() throws Exception {
		String storedCalledNumber = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_CALLED_NUMBER, "  200");
		Assertions.assertEquals("200", storedCalledNumber, "Leading whitespace must not reach the stored CalledNumber");

		String storedCallingNumber = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_CALLING_NUMBER, "100\t");
		Assertions.assertEquals("100", storedCallingNumber, "Trailing whitespace must not reach the stored CallingNumber");
	}

	/**
	 * The length check runs against the stripped value, so a value that only exceeds the cap because of
	 * its padding is accepted rather than rejected.
	 */
	@Test
	void testControlProperty_valueWithinCapAfterStrippingIsAccepted() throws Exception {
		int maxLength = Constant.CALL_DIAGNOSTICS_DESTINATION_MAX_LENGTH;
		String atCap = "x".repeat(maxLength);
		String padded = " " + atCap + " ";
		Assertions.assertTrue(padded.length() > maxLength, "The padded value must exceed the cap before stripping");

		String stored = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_DESTINATION, padded);
		Assertions.assertEquals(atCap, stored, "A value that fits the cap once stripped must be accepted");
		Assertions.assertEquals(maxLength, stored.length());
	}

	@Test
	void testControlProperty_valueOverCapWithoutPaddingIsRejected() throws Exception {
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String overCap = "x".repeat(Constant.CALL_DIAGNOSTICS_DESTINATION_MAX_LENGTH + 1);

		this.communicator.getMultipleStatistics();
		var exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> this.communicator.controlProperty(new ControllableProperty(destinationKey, overCap, null)),
				"A value over the cap with nothing to strip must still be rejected");
		Assertions.assertTrue(exception.getMessage().contains(Constant.CALL_DIAGNOSTICS_DESTINATION),
				"The rejection message should name the field; got: " + exception.getMessage());
	}

	@Test
	void testControlProperty_whitespaceOnlyInputStoresEmptyString() throws Exception {
		String stored = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_DESTINATION, "   ");
		Assertions.assertEquals("", stored, "A whitespace-only Destination must be stored as an empty string");
	}

	/**
	 * Stripping is confined to the ends of the value - no format validation was introduced, so internal
	 * whitespace is forwarded untouched.
	 */
	@Test
	void testControlProperty_internalWhitespaceIsPreserved() throws Exception {
		String stored = this.stageAndReadBack(Constant.CALL_DIAGNOSTICS_DESTINATION, " 10.4.2 19.229 ");
		Assertions.assertEquals("10.4.2 19.229", stored, "Only surrounding whitespace may be stripped");
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
	 * {@code setDisplayPropertyGroups} parses, filters and stores the supplied names without contacting the
	 * device, so the tests below deliberately skip both {@code init()} and the shared {@link #communicator}
	 * fixture and build their own instance instead - they are the only tests in this class that pass with
	 * nothing listening on localhost:8083. State is asserted through {@code getDisplayPropertyGroups()}
	 * (which joins the stored list on {@code ","}) rather than through the warnings the setter logs.
	 */
	@Test
	void testSetDisplayPropertyGroups_withSingleValidGroup() throws Exception {
		var localCommunicator = new AudioCodesMediantCommunicator();
		localCommunicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);

		Assertions.assertEquals(Constant.CALL_LOAD_STATISTICS_GROUP, localCommunicator.getDisplayPropertyGroups());
	}

	/**
	 * A supported name alongside an unsupported one must keep the supported name and drop only the
	 * unsupported one - crucially without tripping the "nothing matched" fallback, which would enable
	 * every group and mask the bad entry. Surrounding whitespace is stripped, and the caller's ordering
	 * is preserved.
	 */
	@Test
	void testSetDisplayPropertyGroups_withUnsupportedGroupAlongsideValidOnes() throws Exception {
		var localCommunicator = new AudioCodesMediantCommunicator();
		localCommunicator.setDisplayPropertyGroups(
				Constant.CALL_LOAD_STATISTICS_GROUP + ", Bogus, " + Constant.MEDIA_STATISTICS_GROUP);

		Assertions.assertEquals(Constant.CALL_LOAD_STATISTICS_GROUP + "," + Constant.MEDIA_STATISTICS_GROUP,
				localCommunicator.getDisplayPropertyGroups(),
				"Unsupported names should be dropped while supported ones survive, without falling back to "
						+ Constant.CALL_STATS_ALL_GROUPS);
	}

	/**
	 * Matching against {@link Constant#SUPPORTED_PROPERTY_GROUPS} is case-sensitive, so a name that differs
	 * only in case is unsupported. When that leaves nothing, the setter falls back to
	 * {@link Constant#CALL_STATS_ALL_GROUPS} - which enables every group. This is deliberate, and this test
	 * pins it so the behaviour cannot change silently.
	 */
	@Test
	void testSetDisplayPropertyGroups_withAllNamesUnsupportedFallsBackToAll() throws Exception {
		var localCommunicator = new AudioCodesMediantCommunicator();
		localCommunicator.setDisplayPropertyGroups("callloadstatistics, MEDIASTATISTICS");

		Assertions.assertEquals(Constant.CALL_STATS_ALL_GROUPS, localCommunicator.getDisplayPropertyGroups(),
				"Every supplied name was unsupported, so the setter should fall back to " + Constant.CALL_STATS_ALL_GROUPS);
	}

	/**
	 * Blank input takes the early return and clears the list outright - it must NOT reach the
	 * {@link Constant#CALL_STATS_ALL_GROUPS} fallback, since "display nothing" and "display everything"
	 * are opposite outcomes.
	 */
	@Test
	void testSetDisplayPropertyGroups_withBlankInputClearsTheList() throws Exception {
		var localCommunicator = new AudioCodesMediantCommunicator();

		localCommunicator.setDisplayPropertyGroups("");
		Assertions.assertEquals("", localCommunicator.getDisplayPropertyGroups(), "An empty string should clear the list");

		localCommunicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		localCommunicator.setDisplayPropertyGroups(null);
		Assertions.assertEquals("", localCommunicator.getDisplayPropertyGroups(), "Null should clear a previously populated list");

		localCommunicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		localCommunicator.setDisplayPropertyGroups("   ");
		Assertions.assertEquals("", localCommunicator.getDisplayPropertyGroups(), "Whitespace-only input should clear the list");
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
	 * <p>
	 * The selection is configured as bare property names, without the group prefix the statistics keys
	 * themselves carry, since resolving the one to the other is part of what this exercises.
	 */
	@Test
	void testGetMultipleStatistics_withHistoricalProperties() throws Exception {
		String answerSeizureRatioProperty = "AnswerSeizureRatio(%)";
		String activeSessionsProperty = "ActiveSessions";
		String answerSeizureRatioKey = Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + answerSeizureRatioProperty;
		String activeSessionsKey = Constant.CALL_LOAD_STATISTICS_GROUP + Constant.HASH + activeSessionsProperty;
		String networkEffectivenessRatioKey = Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + "NetworkEffectivenessRatio(%)";

		var historicalCommunicator = new AudioCodesMediantCommunicator();
		historicalCommunicator.setHost("localhost");
		historicalCommunicator.setPort(8083);
		historicalCommunicator.setLogin("admin");
		historicalCommunicator.setPassword("admin");
		historicalCommunicator.setDisplayPropertyGroups(
				Constant.CALL_QUALITY_STATISTICS_GROUP + "," + Constant.CALL_LOAD_STATISTICS_GROUP);
		historicalCommunicator.setHistoricalProperties(answerSeizureRatioProperty + "," + activeSessionsProperty);
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

	/**
	 * The dial guard is exercised against forced adapter state rather than a live call: the simulator's
	 * test calls end on their own within a poll or two, so a second {@code Start} racing a real session
	 * would be timing-dependent. {@link Constant#CALL_DIAGNOSTICS_DISCONNECTED} is used for the terminal
	 * case so these track the adapter's own definition of a finished call rather than restating it.
	 */
	@Test
	void testControlProperty_startOnFreshAdapterIsAllowed() throws Exception {
		this.stageCallDiagnosticFields();
		Assertions.assertNull(this.readCallDiagnosticSessionId(), "A fresh adapter must not be holding a session");
		Assertions.assertFalse(this.readIsCallDiagnosticStoppable(), "Stop must not be offered before the first dial");

		Assertions.assertDoesNotThrow(
				() -> this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_START), "1", null)),
				"Start on a never-dialled adapter must not be refused");
		Assertions.assertNotNull(this.readCallDiagnosticSessionId(), "Start on a fresh adapter must dial and establish a session");
	}

	@Test
	void testControlProperty_startRefusedWhileSessionIsHeldAndCallIsLive() throws Exception {
		String heldSessionId = "577121441";
		String liveStatus = "Connected";
		this.stageCallDiagnosticFields();
		this.forceCallDiagnosticState(heldSessionId, liveStatus);
		Assertions.assertTrue(this.readIsCallDiagnosticStoppable(), "A live call must be stoppable - the guard keys off this predicate");

		var exception = Assertions.assertThrows(IllegalStateException.class,
				() -> this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_START), "1", null)),
				"Start must be refused while a tracked call has not finished");
		Assertions.assertTrue(exception.getMessage().contains(heldSessionId),
				"The refusal should name the held session id; got: " + exception.getMessage());
		Assertions.assertTrue(exception.getMessage().contains(liveStatus),
				"The refusal should name the current status; got: " + exception.getMessage());

		//	Refused rather than dialled, so the held session is still the one being tracked.
		Assertions.assertEquals(heldSessionId, this.readCallDiagnosticSessionId(), "A refused Start must not replace the held session");
		Assertions.assertTrue(this.readIsCallDiagnosticStoppable(), "Stop visibility must be unaffected by a refused Start");
	}

	@Test
	void testControlProperty_startAllowedWhenDisconnectedButSessionNotYetPurged() throws Exception {
		String staleSessionId = "577121441";
		this.stageCallDiagnosticFields();
		this.forceCallDiagnosticState(staleSessionId, Constant.CALL_DIAGNOSTICS_DISCONNECTED);
		Assertions.assertFalse(this.readIsCallDiagnosticStoppable(), "A disconnected call must not be stoppable");

		Assertions.assertDoesNotThrow(
				() -> this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_START), "1", null)),
				"A finished call whose session the device has not purged yet must not block a new dial");

		//	It dialled rather than being quietly skipped: the stale session was replaced by a new one.
		Assertions.assertNotNull(this.readCallDiagnosticSessionId(), "A permitted Start must establish a session");
		Assertions.assertNotEquals(staleSessionId, this.readCallDiagnosticSessionId(),
				"A permitted Start must dial and replace the stale session id");
	}

	/**
	 * {@code Stop} visibility stays keyed solely on the call status, independently of whether a session is
	 * still held - the dial guard reads the same predicate but must not alter what it reports.
	 */
	@Test
	void testCallDiagnostics_stopVisibilityIsUnchangedByTheDialGuard() throws Exception {
		this.communicator.getMultipleStatistics();

		this.forceCallDiagnosticState(null, Constant.CALL_DIAGNOSTICS_NOT_DIALED);
		Assertions.assertFalse(this.readIsCallDiagnosticStoppable(), "Stop is hidden before the first dial");

		this.forceCallDiagnosticState("577121441", "Connected");
		Assertions.assertTrue(this.readIsCallDiagnosticStoppable(), "Stop is shown while a call is live");

		this.forceCallDiagnosticState("577121441", Constant.CALL_DIAGNOSTICS_DISCONNECTED);
		Assertions.assertFalse(this.readIsCallDiagnosticStoppable(), "Stop is hidden once disconnected, whether or not the session is still held");

		this.forceCallDiagnosticState(null, Constant.CALL_DIAGNOSTICS_DISCONNECTED);
		Assertions.assertFalse(this.readIsCallDiagnosticStoppable(), "Stop is hidden once disconnected and the session has been purged");
	}

	/**
	 * Stages {@code value} against the given {@code CallDiagnostics} property and returns what the
	 * adapter reports back for it on the next polling cycle - i.e. both the stored value and the one
	 * Symphony would display.
	 */
	private String stageAndReadBack(String propertyName, Object value) throws Exception {
		String key = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + propertyName;
		//	localExtendedStatistics has to exist before controlProperty reflects the change into it.
		this.communicator.getMultipleStatistics();
		this.communicator.controlProperty(new ControllableProperty(key, value, null));

		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		return statistics.getStatistics().get(key);
	}

	private String callDiagnosticKey(String propertyName) {
		return Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + propertyName;
	}

	/**
	 * Stages the three values a dial requires, so a subsequent {@code Start} reaches the session guard
	 * instead of being turned away by the blank-field check ahead of it.
	 */
	private void stageCallDiagnosticFields() throws Exception {
		this.communicator.getMultipleStatistics();
		this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_CALLED_NUMBER), "200", null));
		this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_CALLING_NUMBER), "100", null));
		this.communicator.controlProperty(new ControllableProperty(this.callDiagnosticKey(Constant.CALL_DIAGNOSTICS_DESTINATION), "10.4.219.229", null));
	}

	/**
	 * Sets the two fields the dial guard reads. Assigned directly rather than reached by dialling, because
	 * the simulator's test calls are short-lived: driving these states through real calls would make the
	 * assertions depend on how quickly a call ends. No polling cycle may run after this - a refresh would
	 * overwrite the forced status with whatever the device currently reports.
	 */
	private void forceCallDiagnosticState(String sessionId, String status) throws Exception {
		Field sessionIdField = AudioCodesMediantCommunicator.class.getDeclaredField("callDiagnosticSessionId");
		sessionIdField.setAccessible(true);
		sessionIdField.set(this.communicator, sessionId);

		Field statusField = AudioCodesMediantCommunicator.class.getDeclaredField("callDiagnosticStatus");
		statusField.setAccessible(true);
		statusField.set(this.communicator, status);
	}

	private String readCallDiagnosticSessionId() throws Exception {
		Field sessionIdField = AudioCodesMediantCommunicator.class.getDeclaredField("callDiagnosticSessionId");
		sessionIdField.setAccessible(true);
		return (String) sessionIdField.get(this.communicator);
	}

	private boolean readIsCallDiagnosticStoppable() throws Exception {
		Method stoppable = AudioCodesMediantCommunicator.class.getDeclaredMethod("isCallDiagnosticStoppable");
		stoppable.setAccessible(true);
		return (boolean) stoppable.invoke(this.communicator);
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
