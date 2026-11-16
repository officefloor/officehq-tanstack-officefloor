package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (null: from the beginning of time)
 *  through its to-date (null: no end). Version is how many versions the grade and from-date has had on the sheet: each
 *  rate set for them keeps the one it replaces as an older version, this one being the newest (the one in force). */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version) {
    /** A rate as set, before it is stored (its version is given when stored). */
    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 1);
    }

    /** This rate as the given version. */
    public Rate withVersion(int version) {
        return new Rate(sheetId, grade, rate, from, to, version);
    }
}
