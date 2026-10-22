package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate set: who is making it, the grade, and the percent with two decimals. */
public record RateInput(String user, String grade, BigDecimal pct) {
}
