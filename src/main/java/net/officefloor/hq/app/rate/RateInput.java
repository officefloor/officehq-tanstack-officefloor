package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate: the grade, the percent scaled to two decimals, and its from-date (null when none). */
public record RateInput(String grade, BigDecimal rate, LocalDate from) {
}
