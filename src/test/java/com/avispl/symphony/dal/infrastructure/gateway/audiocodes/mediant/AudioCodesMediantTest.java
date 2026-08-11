/**
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.FailedLoginException;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallLoadStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallQualityStatsProperty;

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

	/**
	 * Note: gauge KPIs are reported as dynamic statistics and are therefore absent from the regular
	 * statistics map. Every {@link CallQualityStatsProperty} KPI is a gauge, so CallQualityStatistics is the
	 * only group asserted against {@code getDynamicStatistics()}. {@link CallLoadStatsProperty} is mixed -
	 * ActiveSessions is a gauge, the two Total counters are not - so its regular map stays non-empty and it
	 * is still asserted against {@code getStatistics()}, as are the remaining counter-only groups. If a
	 * group's KPIs are ever <em>all</em> flipped to {@code gauge = true}, move it across too; a fully-gauge
	 * group's regular map is empty and would trip the {@code isNotEmpty} assertion below.
	 */
	@Test
	void testGetMultipleStatistics_withCallStatsGroups() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);

		for (String group : this.callStatsGroups()) {
			boolean gaugeGroup = Constant.CALL_QUALITY_STATISTICS_GROUP.equals(group);
			var groupStats = this.filterGroupStatistics(gaugeGroup ? statistics.getDynamicStatistics() : statistics.getStatistics(), group);
			Assertions.assertTrue(MapUtils.isNotEmpty(groupStats), "Expected non-empty stats for group " + group);
			groupStats.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
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

	@Test
	void testGetMultipleStatistics_withGaugeKpisInDynamicStatistics() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);

		//	The device reports null for these when too few calls occurred in the sampling window, so assert the
		//	routing rule rather than the presence of a sample: a gauge is either a numeric dynamic statistic or
		//	an "N/A" regular one, and never both at once.
		for (String gaugeKey : this.gaugeKeys()) {
			boolean inDynamic = statistics.getDynamicStatistics().containsKey(gaugeKey);
			boolean inRegular = statistics.getStatistics().containsKey(gaugeKey);
			Assertions.assertNotEquals(inDynamic, inRegular, "A gauge KPI must appear in exactly one of the two maps: " + gaugeKey);
			if (inDynamic) {
				Assertions.assertTrue(Util.isNumeric(statistics.getDynamicStatistics().get(gaugeKey)),
						"A dynamic statistic must be a numeric sample: " + gaugeKey);
			} else {
				Assertions.assertEquals(Constant.NOT_AVAILABLE, statistics.getStatistics().get(gaugeKey));
			}
		}
	}

	@Test
	void testGetMultipleStatistics_withCounterKpisExcludedFromDynamicStatistics() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);

		statistics.getDynamicStatistics().keySet().forEach(key ->
				Assertions.assertFalse(key.endsWith("Total"), "Cumulative counter must not be trended: " + key));
		//	Nothing beyond the declared gauges may reach the dynamic map.
		Assertions.assertTrue(this.gaugeKeys().containsAll(statistics.getDynamicStatistics().keySet()),
				"Unexpected dynamic statistics: " + statistics.getDynamicStatistics().keySet());
	}

	@Test
	void testGetMultipleStatistics_withNoGroupsEnabled() throws Exception {
		var noGroupsCommunicator = new AudioCodesMediantCommunicator();
		noGroupsCommunicator.setHost("localhost");
		noGroupsCommunicator.setPort(8083);
		noGroupsCommunicator.setLogin("admin");
		noGroupsCommunicator.setPassword("admin");
		noGroupsCommunicator.setDisplayPropertyGroups("");
		noGroupsCommunicator.init();
		try {
			var statistics = (ExtendedStatistics) noGroupsCommunicator.getMultipleStatistics().get(0);
			for (String group : this.callStatsGroups()) {
				Assertions.assertTrue(MapUtils.isEmpty(this.filterGroupStatistics(statistics.getStatistics(), group)),
						"No KPI should be reported for " + group + " when no group is enabled");
			}
			Assertions.assertTrue(MapUtils.isEmpty(statistics.getDynamicStatistics()),
					"No dynamic statistics should be reported when no group is enabled");
		} finally {
			noGroupsCommunicator.disconnect();
			noGroupsCommunicator.destroy();
		}
	}

	/**
	 * Covers the null-value routing rule directly: the device cannot be made to report a null KPI on demand,
	 * so the live tests above can only assert whichever branch the device happens to exercise.
	 */
	@Test
	void testPutKpiValue_withNullAndNumericValues() {
		Map<String, String> stats = new HashMap<>();
		Map<String, String> dynamicStats = new HashMap<>();

		Util.putKpiValue(stats, dynamicStats, "Group#Gauge", null, true);
		Assertions.assertEquals(Constant.NOT_AVAILABLE, stats.get("Group#Gauge"));
		Assertions.assertFalse(dynamicStats.containsKey("Group#Gauge"), "A null gauge must be omitted from the dynamic map");

		Util.putKpiValue(stats, dynamicStats, "Group#NonNumericGauge", "unavailable", true);
		Assertions.assertFalse(dynamicStats.containsKey("Group#NonNumericGauge"), "A non-numeric gauge must be omitted from the dynamic map");

		Util.putKpiValue(stats, dynamicStats, "Group#NumericGauge", "1.5", true);
		Assertions.assertEquals("1.5", dynamicStats.get("Group#NumericGauge"));
		Assertions.assertFalse(stats.containsKey("Group#NumericGauge"), "A trended gauge must not be duplicated into the regular map");

		Util.putKpiValue(stats, dynamicStats, "Group#Counter", "42", false);
		Assertions.assertEquals("42", stats.get("Group#Counter"));
		Assertions.assertFalse(dynamicStats.containsKey("Group#Counter"), "A counter must never be trended");

		Util.putKpiValue(stats, dynamicStats, "Group#NullCounter", null, false);
		Assertions.assertEquals(Constant.NOT_AVAILABLE, stats.get("Group#NullCounter"));
	}

	/**
	 * The fully-qualified keys of every KPI declared as a gauge - i.e. the only keys allowed to reach
	 * the dynamic statistics map. Add a group here as soon as any of its KPIs is marked {@code gauge = true}.
	 */
	private Set<String> gaugeKeys() {
		return Stream.concat(
						this.gaugeKeys(Constant.CALL_LOAD_STATISTICS_GROUP, CallLoadStatsProperty.values()),
						this.gaugeKeys(Constant.CALL_QUALITY_STATISTICS_GROUP, CallQualityStatsProperty.values()))
				.collect(Collectors.toSet());
	}

	private Stream<String> gaugeKeys(String groupName, KpiProperty[] properties) {
		return Arrays.stream(properties)
				.filter(KpiProperty::isGauge)
				.map(property -> groupName + Constant.HASH + property.getName());
	}

	private List<String> callStatsGroups() {
		return List.of(
				Constant.CALL_LOAD_STATISTICS_GROUP,
				Constant.CALL_QUALITY_STATISTICS_GROUP,
				Constant.CALL_TERMINATION_STATISTICS_GROUP,
				Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP,
				Constant.CALL_CAPACITY_STATISTICS_GROUP,
				Constant.CALL_ROUTING_STATISTICS_GROUP,
				Constant.CALL_TRAFFIC_STATISTICS_GROUP);
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
