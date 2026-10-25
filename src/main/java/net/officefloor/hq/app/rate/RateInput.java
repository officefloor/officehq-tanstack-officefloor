package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate: the grade, and the percent scaled to two decimals. */
public record RateInput(String grade, BigDecimal rate) {
}
