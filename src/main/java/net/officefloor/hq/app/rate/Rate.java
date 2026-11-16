package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (null: from the beginning of time)
 *  through its to-date (null: no end). Version is how many versions the grade and from-date has had on the sheet: each
 *  rate set for them keeps the one it replaces as an older version, this one being the newest (the one in force).
 *  After is set on a forward-only rate (one saved with a from-date on or after the sheet's today): the highest added
 *  sequence of any claim when it was saved, so it applies only to claims added later (see #appliesTo); null when it
 *  applies to every claim. */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version, Long after) {
    /** A rate as set, before it is stored (its version and whether it is forward-only are given when stored). */
    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 1, null);
    }

    /** This rate as the given version. */
    public Rate withVersion(int version) {
        return new Rate(sheetId, grade, rate, from, to, version, after);
    }

    /** This rate as applying only to claims added after the given added sequence (null: to every claim). */
    public Rate withAfter(Long after) {
        return new Rate(sheetId, grade, rate, from, to, version, after);
    }

    /** Whether the rate applies to a claim with the given added sequence (null: a claim not yet added, which is added
     *  after every rate): always, unless it is forward-only and the claim was added before it was saved. */
    public boolean appliesTo(Long seq) {
        return after == null || seq == null || seq > after;
    }
}
