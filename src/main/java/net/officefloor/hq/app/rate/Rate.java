package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate on a sheet: a percent with two decimals, in force from a date (null: from the beginning of time)
 * through a to-date (null: with no end). */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
}
