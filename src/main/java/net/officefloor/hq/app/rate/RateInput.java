package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated rate body: a grade of A, B or C, a percent of 0.00 or more and a from-date (null: from the start) and a to-date (null: without end). */
public record RateInput(String grade, BigDecimal pct, LocalDate from, LocalDate to) {
}
