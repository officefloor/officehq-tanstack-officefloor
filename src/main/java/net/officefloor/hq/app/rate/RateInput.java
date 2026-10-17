package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate to set: the grade, its percent with two decimals, and its from-date (null: from the start). */
public record RateInput(String grade, BigDecimal rate, LocalDate from) {
}
