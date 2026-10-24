package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate form: the grade, its percent with two decimals and its from-date (null when none). */
public record RateInput(String grade, BigDecimal pct, LocalDate from) {
}
