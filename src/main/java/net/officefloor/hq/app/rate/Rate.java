package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A sheet's rate for a grade: a percent with two decimals, in force from {@code from} (null: from the beginning of
 * time) through {@code to} (null: with no end). {@code version} is how many versions the grade and from-date's rate has
 * had: setting it again keeps the value replaced as an older version, and this is always the newest.
 */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version) {

    /** A rate as typed, before it is stored: its first version. */
    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 1);
    }
}
