package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, in force from {@code from} (null: from the beginning of time). */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from) {
}
