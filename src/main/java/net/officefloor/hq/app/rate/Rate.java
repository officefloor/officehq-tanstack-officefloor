package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (null: from the
 *  beginning of time). */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from) {

    /** Whether this rate could apply on the date: it has no from-date, or its from-date is on or before it. */
    public boolean appliesOn(LocalDate date) {
        return from == null || !from.isAfter(date);
    }
}
