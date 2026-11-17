/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.Alarms;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link Util}. Unlike {@code AudioCodesMediantTest}, these need no device or
 * simulator - they exercise pure value handling only.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class UtilTest {

	@Test
	void testIsNumeric_withValidInt() {
		Assertions.assertTrue(Util.isNumeric("42"));
		Assertions.assertTrue(Util.isNumeric("0"));
	}

	@Test
	void testIsNumeric_withValidDecimal() {
		Assertions.assertTrue(Util.isNumeric("3.5"));
		Assertions.assertTrue(Util.isNumeric("0.0"));
	}

	@Test
	void testIsNumeric_withNegative() {
		Assertions.assertTrue(Util.isNumeric("-7"));
		Assertions.assertTrue(Util.isNumeric("-0.25"));
	}

	/**
	 * {@link Constant#NOT_AVAILABLE} is the value every failed or empty KPI response collapses to, so
	 * rejecting it is what keeps a failed poll out of the dynamic statistics map entirely.
	 */
	@Test
	void testIsNumeric_withNotAvailable() {
		Assertions.assertFalse(Util.isNumeric(Constant.NOT_AVAILABLE));
		Assertions.assertFalse(Util.isNumeric("N/A"));
	}

	@Test
	void testIsNumeric_withNull() {
		Assertions.assertFalse(Util.isNumeric(null));
	}

	@Test
	void testIsNumeric_withEmpty() {
		Assertions.assertFalse(Util.isNumeric(""));
	}

	@Test
	void testIsNumeric_withWhitespaceOnly() {
		Assertions.assertFalse(Util.isNumeric("   "));
		Assertions.assertFalse(Util.isNumeric("\t"));
	}

	/**
	 * A padded value is rejected rather than accepted and then stored with its padding intact - the
	 * check deliberately does not strip, since the value is recorded exactly as received.
	 */
	@Test
	void testIsNumeric_withSurroundingWhitespace() {
		Assertions.assertFalse(Util.isNumeric(" 42"));
		Assertions.assertFalse(Util.isNumeric("42 "));
		Assertions.assertFalse(Util.isNumeric(" 42 "));
	}

	/**
	 * A digit string too long for a {@code double} parses to {@link Double#POSITIVE_INFINITY} rather
	 * than throwing, so the pattern match alone would let it through; the finiteness check is what
	 * rejects it.
	 */
	@Test
	void testIsNumeric_withDigitStringOverflowingToInfinity() {
		String overflowing = "9".repeat(310);
		Assertions.assertTrue(Double.isInfinite(Double.parseDouble(overflowing)),
				"Precondition: this literal must overflow to infinity for the test to be meaningful");
		Assertions.assertFalse(Util.isNumeric(overflowing));
	}

	/**
	 * Values a bare {@link Double#parseDouble(String)} would accept but which are unusable as a metric.
	 * A cumulative counter past {@link Integer#MAX_VALUE} must still be accepted, since
	 * {@code isInt} - the check this one extends - rejects it.
	 */
	@Test
	void testIsNumeric_withNonNumericAndOversizedForms() {
		Assertions.assertFalse(Util.isNumeric("3f"));
		Assertions.assertFalse(Util.isNumeric("1.5d"));
		Assertions.assertFalse(Util.isNumeric("0x1p3"));
		Assertions.assertFalse(Util.isNumeric("NaN"));
		Assertions.assertFalse(Util.isNumeric("Infinity"));
		Assertions.assertFalse(Util.isNumeric("true"));
		Assertions.assertTrue(Util.isNumeric("4294967296"));
	}

	/**
	 * Pins the alarm property names agreed in SYAL-3997: the summary group keeps its {@code ActiveAlarms}
	 * name and {@code Count}, its source list is {@code Source}, and each alarm is reported under
	 * {@code ActiveAlarm_<id>} with a {@code Timestamp} taken from the device's {@code date} field.
	 */
	@Test
	void testGenerateActiveAlarmsProperties_usesRenamedKeys() throws Exception {
		Alarms alarm = new ObjectMapper().readValue(
				"{\"id\":7,\"description\":\"Fan\",\"severity\":\"major\",\"source\":\"Board#1\",\"date\":\"2026-11-11T10:00:00Z\"}",
				Alarms.class);

		Map<String, String> properties = Util.generateActiveAlarmsProperties(List.of(alarm));

		Assertions.assertEquals("1", properties.get("ActiveAlarms#Count"));
		Assertions.assertTrue(properties.containsKey("ActiveAlarms#Severity"));
		Assertions.assertTrue(properties.containsKey("ActiveAlarms#Source"));
		Assertions.assertEquals("2026-11-11T10:00:00Z", properties.get("ActiveAlarm_7#Timestamp"));
		Assertions.assertTrue(properties.containsKey("ActiveAlarm_7#Description"));
		Assertions.assertTrue(properties.containsKey("ActiveAlarm_7#Severity"));
		Assertions.assertTrue(properties.containsKey("ActiveAlarm_7#Source"));

		Assertions.assertFalse(properties.containsKey("ActiveAlarms#Sources"));
		Assertions.assertTrue(properties.keySet().stream().noneMatch(key -> key.startsWith("ActiveAlarms_")),
				"Per-alarm groups no longer use the ActiveAlarms_ prefix");
		Assertions.assertTrue(properties.keySet().stream().noneMatch(key -> key.endsWith("#Date")),
				"The per-alarm Date property is now Timestamp");
	}
}
