package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet's rate for a grade: a percent with two decimals, in force from its from-date (null: from the
 *  beginning of time) through its to-date (null: with no end); its version is how many have been set for the grade
 *  from that from-date, this the newest. A forward-only rate (saved with a from-date on or after the sheet's today)
 *  carries the highest claim id when it was saved (after; null: it applies to every claim): it applies only to claims
 *  added after it. */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, int version, Long after) {

    /** Whether this rate reaches the claim: it is not forward-only, or the claim was added after it was saved (a claim
     *  id of 0 or less: no claim in particular, or one not yet added, which every rate reaches). */
    public boolean reaches(long claimId) {
        return after == null || claimId <= 0 || claimId > after;
    }

    /** Whether this rate could apply on the date: it has no from-date, or its from-date is on or before it. */
    public boolean appliesOn(LocalDate date) {
        return from == null || !from.isAfter(date);
    }

    /** Whether the date is after this rate's to-date: the rate has ended by then. */
    public boolean endedBy(LocalDate date) {
        return to != null && to.isBefore(date);
    }
}
