package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate body: a grade of A, B or C and a percent of 0.00 or more. */
public record RateInput(String grade, BigDecimal pct) {
}
