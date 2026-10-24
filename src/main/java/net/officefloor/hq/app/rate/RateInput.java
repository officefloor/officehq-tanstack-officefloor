package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate form: the grade and its percent with two decimals. */
public record RateInput(String grade, BigDecimal pct) {
}
