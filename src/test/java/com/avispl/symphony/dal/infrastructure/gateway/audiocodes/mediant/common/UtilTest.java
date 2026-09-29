/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
