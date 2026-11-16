package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A grade's rate once validated, and who sets it. */
public record RateValues(String grade, BigDecimal rate, String by) {
}
