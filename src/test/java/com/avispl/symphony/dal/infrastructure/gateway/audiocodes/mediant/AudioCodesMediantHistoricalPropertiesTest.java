/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

/**
 * Tests the {@code historicalProperties} selection in
 * {@link AudioCodesMediantCommunicator#extractHistoricalProperties(Map)}.
 * <p>
 * Deliberately separate from {@code AudioCodesMediantTest}: the communicator here is constructed but
 * never initialised, so these tests exercise the selection against a hand-built statistics map and
 * need no device or simulator. Keys are spelled out literally, exactly as the adapter emits them -
 * group prefix, {@code #} separator and unit suffix included - since exact matching of that whole
 * string is the behaviour under test.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantHistoricalPropertiesTest {
	private static final String RATIO_KEY = "CallQualityStatistics#AnswerSeizureRatio(%)";
	private static final String JITTER_KEY = "MediaStatistics#MediaJitterIn(ms)";
	private static final String SESSIONS_KEY = "CallLoadStatistics#ActiveSessions";

	private AudioCodesMediantCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		this.communicator = new AudioCodesMediantCommunicator();
	}

	/**
	 * A numeric value is copied into the dynamic map and left in place in the static one - a selected
	 * property is reported as both an extended property and a dynamic statistic.
	 */
	@Test
	void testExtractHistoricalProperties_copiesNumericValueIntoDynamic() {
		this.communicator.setHistoricalProperties(RATIO_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, "97");
		stats.put(SESSIONS_KEY, "12");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertEquals(Map.of(RATIO_KEY, "97"), dynamicStats);
		Assertions.assertEquals("97", stats.get(RATIO_KEY), "A property reported dynamically must also remain in the static map");
		Assertions.assertEquals(Map.of(RATIO_KEY, "97", SESSIONS_KEY, "12"), stats);
	}

	/**
	 * A fractional value must survive the copy - {@code isInt}, the check {@code isNumeric} extends,
	 * would reject it.
	 */
	@Test
	void testExtractHistoricalProperties_copiesDecimalValueIntoDynamic() {
		this.communicator.setHistoricalProperties(RATIO_KEY + "," + JITTER_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, "99.5");
		stats.put(JITTER_KEY, "4");
		Map<String, String> untouched = Map.copyOf(stats);

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertEquals(Map.of(RATIO_KEY, "99.5", JITTER_KEY, "4"), dynamicStats);
		Assertions.assertEquals(untouched, stats, "stats is never modified");
	}

	@Test
	void testExtractHistoricalProperties_leavesUnlistedKeyInStatic() {
		this.communicator.setHistoricalProperties(RATIO_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(SESSIONS_KEY, "12");
		stats.put(JITTER_KEY, "4");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(Map.of(SESSIONS_KEY, "12", JITTER_KEY, "4"), stats);
	}

	/**
	 * {@link Constant#NOT_AVAILABLE} is what every failed or empty KPI response collapses to. It is not
	 * a usable data point, so it is kept out of the dynamic map - but it stays in {@code stats} and is
	 * still reported as an extended property, so the operator sees that the KPI reported nothing usable.
	 */
	@Test
	void testExtractHistoricalProperties_keepsNotAvailableInStaticMap() {
		this.communicator.setHistoricalProperties(RATIO_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, Constant.NOT_AVAILABLE);
		stats.put(SESSIONS_KEY, "12");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty(), "N/A is not a usable data point");
		Assertions.assertEquals(Map.of(RATIO_KEY, Constant.NOT_AVAILABLE, SESSIONS_KEY, "12"), stats,
				"A non-numeric value stays in the static map");
	}

	/**
	 * A listed key naming a property not collected this cycle - its group is disabled, or the key is
	 * misspelled - is skipped rather than emitted as an empty or {@code N/A} entry.
	 */
	@Test
	void testExtractHistoricalProperties_withListedKeyAbsentThisCycle() {
		this.communicator.setHistoricalProperties(JITTER_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(SESSIONS_KEY, "12");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(Map.of(SESSIONS_KEY, "12"), stats);
	}

	@Test
	void testExtractHistoricalProperties_withHistoricalPropertiesUnset() {
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, "97");
		stats.put(SESSIONS_KEY, "12");
		Map<String, String> untouched = Map.copyOf(stats);

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(untouched, stats, "Behaviour must be unchanged when historicalProperties is never set");
	}

	/**
	 * A blank value fails closed - it selects nothing, rather than falling back to selecting
	 * everything the way {@code displayPropertyGroups} does.
	 */
	@Test
	void testExtractHistoricalProperties_withHistoricalPropertiesBlank() {
		this.communicator.setHistoricalProperties("   ");
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, "97");
		stats.put(SESSIONS_KEY, "12");
		Map<String, String> untouched = Map.copyOf(stats);

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(untouched, stats);
	}

	/**
	 * A fresh map is returned per call, so a property that stops being collected does not linger from
	 * an earlier cycle.
	 */
	@Test
	void testExtractHistoricalProperties_returnsFreshMapEachCycle() {
		this.communicator.setHistoricalProperties(RATIO_KEY);
		Map<String, String> firstCycle = new HashMap<>();
		firstCycle.put(RATIO_KEY, "97");
		var firstDynamicStats = this.communicator.extractHistoricalProperties(firstCycle);

		var secondDynamicStats = this.communicator.extractHistoricalProperties(new HashMap<>());

		Assertions.assertEquals(Map.of(RATIO_KEY, "97"), firstDynamicStats);
		Assertions.assertTrue(secondDynamicStats.isEmpty(), "The previous cycle's data point must not carry over");
	}
}
