package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate change: the grade, its percent (two decimals) and who sets it. */
public record RateInput(String grade, BigDecimal pct, String user) {
}
