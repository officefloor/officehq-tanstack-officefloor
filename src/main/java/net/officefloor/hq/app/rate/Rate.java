package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate on a sheet: a percent with two decimals, in force from `from` (null: from the beginning of time)
 *  through `to` (null: with no end). `version` counts the values set for its grade and from-date, this
 *  newest one included (0 for one not yet saved). */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, int version) {
    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        this(sheetId, grade, pct, from, to, 0);
    }
}
