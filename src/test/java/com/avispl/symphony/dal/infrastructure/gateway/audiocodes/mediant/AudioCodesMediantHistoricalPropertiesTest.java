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
 * {@link AudioCodesMediantCommunicator#extractHistoricalProperties(Map)}, and the validation
 * {@link AudioCodesMediantCommunicator#setHistoricalProperties(String)} applies against
 * {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES} before a name ever reaches that selection.
 * <p>
 * Deliberately separate from {@code AudioCodesMediantTest}: the communicator here is constructed but
 * never initialised, so these tests exercise the selection against a hand-built statistics map and
 * need no device or simulator. The configuration is a bare property name - unit suffix included, no
 * group prefix - while the statistics map is keyed the way the adapter emits it, group prefix and
 * {@code #} separator included, since matching a configured name against the part of the key after
 * the hash is the behaviour under test.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantHistoricalPropertiesTest {
	private static final String RATIO_PROPERTY = "AnswerSeizureRatio(%)";
	private static final String JITTER_PROPERTY = "MediaJitterIn(ms)";
	private static final String SESSIONS_PROPERTY = "ActiveSessions";
	private static final String RATIO_KEY = "CallQualityStatistics" + Constant.HASH + RATIO_PROPERTY;
	private static final String JITTER_KEY = "MediaStatistics" + Constant.HASH + JITTER_PROPERTY;
	private static final String SESSIONS_KEY = "CallLoadStatistics" + Constant.HASH + SESSIONS_PROPERTY;

	private AudioCodesMediantCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		this.communicator = new AudioCodesMediantCommunicator();
	}

	/**
	 * A numeric value is copied into the dynamic map - keyed by the full statistics key, not by the
	 * configured name - and left in place in the static one: a selected property is reported as both
	 * an extended property and a dynamic statistic.
	 */
	@Test
	void testExtractHistoricalProperties_copiesNumericValueIntoDynamic() {
		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
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
		this.communicator.setHistoricalProperties(RATIO_PROPERTY + "," + JITTER_PROPERTY);
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
		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
		Map<String, String> stats = new HashMap<>();
		stats.put(SESSIONS_KEY, "12");
		stats.put(JITTER_KEY, "4");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(Map.of(SESSIONS_KEY, "12", JITTER_KEY, "4"), stats);
	}

	/**
	 * The configuration names a property, not a key, so a name two groups shared would select the
	 * property in both, each keyed by its own full statistics key. No two groups currently share a
	 * property name, so the map here is hand-built to cover the rule rather than a real pairing.
	 */
	@Test
	void testExtractHistoricalProperties_selectsSharedPropertyNameInEveryGroup() {
		this.communicator.setHistoricalProperties(SESSIONS_PROPERTY);
		String mediaSessionsKey = "MediaStatistics" + Constant.HASH + SESSIONS_PROPERTY;
		Map<String, String> stats = new HashMap<>();
		stats.put(SESSIONS_KEY, "12");
		stats.put(mediaSessionsKey, "5");
		stats.put(RATIO_KEY, "97");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertEquals(Map.of(SESSIONS_KEY, "12", mediaSessionsKey, "5"), dynamicStats);
	}

	/**
	 * A full group-prefixed key is not a valid entry: the configured value is compared to the part of
	 * the statistics key after the hash, which never carries a group prefix. Since the whitelist holds
	 * bare property names, such a key is now rejected by the setter rather than merely failing to match
	 * later - so the property is not selected either way, but the caller gets a warning about it.
	 */
	@Test
	void testExtractHistoricalProperties_withGroupPrefixedKeyConfigured() {
		this.communicator.setHistoricalProperties(RATIO_KEY);
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, "97");
		Map<String, String> untouched = Map.copyOf(stats);

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertEquals("", this.communicator.getHistoricalProperties(),
				"A group-prefixed key is not a supported name; the property name alone is the config format");
		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(untouched, stats);
	}

	/**
	 * The ungrouped General and Network properties are emitted without a group prefix, and
	 * {@code extractHistoricalProperties} still matches such a key whole - but no ungrouped property is
	 * in {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES}, every supported name belonging to a prefixed
	 * KPI group. Naming one is therefore rejected by the setter and selects nothing, whatever the
	 * statistics map holds. This pins that consequence of the whitelist: what used to be a usable
	 * selection is no longer reachable through configuration.
	 */
	@Test
	void testExtractHistoricalProperties_rejectsUngroupedProperty() {
		String uptimeKey = "SystemUptime(sec)";
		this.communicator.setHistoricalProperties(uptimeKey);
		Map<String, String> stats = new HashMap<>();
		stats.put(uptimeKey, "864000");
		stats.put(RATIO_KEY, "97");
		Map<String, String> untouched = Map.copyOf(stats);

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertEquals("", this.communicator.getHistoricalProperties(),
				"No ungrouped property is supported, so the setter should have dropped the name");
		Assertions.assertTrue(dynamicStats.isEmpty());
		Assertions.assertEquals(untouched, stats);
	}

	/**
	 * {@link Constant#NOT_AVAILABLE} is what every failed or empty KPI response collapses to. It is not
	 * a usable data point, so it is kept out of the dynamic map - but it stays in {@code stats} and is
	 * still reported as an extended property, so the operator sees that the KPI reported nothing usable.
	 * A warning naming the key and the value is logged for it.
	 */
	@Test
	void testExtractHistoricalProperties_keepsNotAvailableInStaticMap() {
		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
		Map<String, String> stats = new HashMap<>();
		stats.put(RATIO_KEY, Constant.NOT_AVAILABLE);
		stats.put(SESSIONS_KEY, "12");

		var dynamicStats = this.communicator.extractHistoricalProperties(stats);

		Assertions.assertTrue(dynamicStats.isEmpty(), "N/A is not a usable data point");
		Assertions.assertEquals(Map.of(RATIO_KEY, Constant.NOT_AVAILABLE, SESSIONS_KEY, "12"), stats,
				"A non-numeric value stays in the static map");
	}

	/**
	 * A listed name matching no property collected this cycle - its group is excluded by
	 * {@code displayPropertyGroups} - is skipped rather than emitted as an empty or {@code N/A} entry.
	 * A misspelled name can no longer get this far; the setter rejects it.
	 */
	@Test
	void testExtractHistoricalProperties_withListedPropertyAbsentThisCycle() {
		this.communicator.setHistoricalProperties(JITTER_PROPERTY);
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
		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
		Map<String, String> firstCycle = new HashMap<>();
		firstCycle.put(RATIO_KEY, "97");
		var firstDynamicStats = this.communicator.extractHistoricalProperties(firstCycle);

		var secondDynamicStats = this.communicator.extractHistoricalProperties(new HashMap<>());

		Assertions.assertEquals(Map.of(RATIO_KEY, "97"), firstDynamicStats);
		Assertions.assertTrue(secondDynamicStats.isEmpty(), "The previous cycle's data point must not carry over");
	}

	/**
	 * {@code setHistoricalProperties} parses, filters and stores the supplied names without contacting
	 * the device, the same way {@code setDisplayPropertyGroups} does. State is asserted through
	 * {@code getHistoricalProperties()} - which joins the stored set on {@code ","} - rather than
	 * through the warning the setter logs, matching the convention the group-selection tests in
	 * {@code AudioCodesMediantTest} follow.
	 */
	@Test
	void testSetHistoricalProperties_withSingleSupportedName() {
		this.communicator.setHistoricalProperties(RATIO_PROPERTY);

		Assertions.assertEquals(RATIO_PROPERTY, this.communicator.getHistoricalProperties());
	}

	/**
	 * A supported name alongside an unsupported one keeps the supported name and drops only the
	 * unsupported one. Surrounding whitespace is stripped, and the caller's ordering survives - the
	 * backing set is a {@code LinkedHashSet} precisely so the getter round-trips what was asked for.
	 */
	@Test
	void testSetHistoricalProperties_withUnsupportedNameAlongsideValidOnes() {
		this.communicator.setHistoricalProperties(RATIO_PROPERTY + ", Bogus, " + JITTER_PROPERTY);

		Assertions.assertEquals(RATIO_PROPERTY + "," + JITTER_PROPERTY, this.communicator.getHistoricalProperties(),
				"Unsupported names should be dropped while supported ones survive, in the order supplied");
	}

	/**
	 * Matching against {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES} is case-sensitive, so a name
	 * differing only in case is unsupported. Unlike {@code setDisplayPropertyGroups}, dropping every
	 * supplied name does not fall back to selecting everything - this setter fails closed. That
	 * difference between the two setters is deliberate, and this test pins it so it cannot change
	 * silently.
	 */
	@Test
	void testSetHistoricalProperties_withAllNamesUnsupportedSelectsNothing() {
		this.communicator.setHistoricalProperties("answerseizureratio(%), MEDIAJITTERIN(MS)");

		Assertions.assertEquals("", this.communicator.getHistoricalProperties(),
				"Every supplied name was unsupported, and there is no 'select everything' fallback");
	}

	/**
	 * The unit suffix is part of the name, so omitting it or supplying the wrong one is unsupported.
	 * This is exactly the typo the whitelist exists to surface: before, either spelling was retained
	 * and silently matched nothing.
	 */
	@Test
	void testSetHistoricalProperties_withMissingOrWrongUnitSuffix() {
		this.communicator.setHistoricalProperties("AnswerSeizureRatio");
		Assertions.assertEquals("", this.communicator.getHistoricalProperties(),
				"The unit suffix is part of the supported name");

		this.communicator.setHistoricalProperties("MediaJitterIn(s)");
		Assertions.assertEquals("", this.communicator.getHistoricalProperties(),
				"A wrong unit suffix does not make a supported name");
	}

	/**
	 * Blank input takes the early return and clears the selection outright. The outcome matches that of
	 * an all-unsupported value - both select nothing - but it stays a distinct early return, since the
	 * warning is only meaningful when the caller actually named something.
	 */
	@Test
	void testSetHistoricalProperties_withBlankInputClearsTheSelection() {
		this.communicator.setHistoricalProperties("");
		Assertions.assertEquals("", this.communicator.getHistoricalProperties(), "An empty string should clear the selection");

		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
		this.communicator.setHistoricalProperties(null);
		Assertions.assertEquals("", this.communicator.getHistoricalProperties(), "Null should clear a previously populated selection");

		this.communicator.setHistoricalProperties(RATIO_PROPERTY);
		this.communicator.setHistoricalProperties("   ");
		Assertions.assertEquals("", this.communicator.getHistoricalProperties(), "Whitespace-only input should clear the selection");
	}

	/**
	 * Every name in {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES} must survive the setter exactly as
	 * supplied: the whitelist and the parsing have to agree, or a name the adapter advertises as
	 * supported would be unusable through configuration. This also catches a name accidentally carrying
	 * whitespace or a stray comma when the set is edited.
	 */
	@Test
	void testSetHistoricalProperties_acceptsEverySupportedName() {
		for (String supported : Constant.SUPPORTED_HISTORICAL_PROPERTIES) {
			this.communicator.setHistoricalProperties(supported);

			Assertions.assertEquals(supported, this.communicator.getHistoricalProperties(),
					"A supported name must survive the setter unchanged: " + supported);
		}
	}
}
