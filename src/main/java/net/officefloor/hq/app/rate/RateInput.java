package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate to set: the grade and its percent, with two decimals. */
public record RateInput(String grade, BigDecimal rate) {
}
