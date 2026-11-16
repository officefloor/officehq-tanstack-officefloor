package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate once validated, its from-date and to-date (null for none), and who sets it. */
public record RateValues(String grade, BigDecimal rate, LocalDate from, LocalDate to, String by) {
}
