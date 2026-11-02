package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate form: the grade, its percent scaled to two decimals, and its from-date (null when
 *  it applies from the beginning of time), and its to-date (null when it has no end). */
public record RateInput(String grade, BigDecimal pct, LocalDate from, LocalDate to) {
}
