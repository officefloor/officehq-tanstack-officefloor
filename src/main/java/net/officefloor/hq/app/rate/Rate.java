package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate on a sheet: a percent with two decimals, in force from `from` (null: from the beginning of time)
 *  through `to` (null: with no end). */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
}
