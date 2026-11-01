package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;

/**
 * A person on a sheet: name (unique on the sheet, ignoring case) and grades (A, B or C), each in force from its
 * from-date; grade is the one in force on the sheet's today. The version is raised by one on every save. The
 * retired-from date (yyyy-mm-dd) is null unless they are retired; once it is set they count as retired, whatever
 * the date. Their holds are listed in the order they were added; their OPEN claims dated within a hold, unless bound
 * by a statement, count in no live total. Their own terms (days) are null when they have none.
 */
public record Person(long id, String name, String grade, long version, String retiredFrom, List<Hold> holds,
        List<GradeFrom> grades, Integer terms) {

    /** Whether the claim, theirs, counts toward the live totals: it counts, unless it is open, not bound by a statement, and dated within one of their holds. */
    public boolean counts(Claim claim) {
        if (!claim.counts()) {
            return false;
        }
        if (claim.status() == ClaimStatus.SETTLED || claim.bound()) {
            return true;
        }
        return holds.stream().noneMatch((h) -> h.covers(claim.date()));
    }

    /**
     * Whether the claim, theirs, counts toward a period ending on the date: by the status it had at the end of that
     * day (OPEN, PENDING or SETTLED; not yet added: no), unless deferred now, or open now, not bound by a statement
     * and dated within one of their holds.
     */
    public boolean countsOn(Claim claim, LocalDate end) {
        ClaimStatus then = claim.statusOn(end);
        if (claim.deferred() || then == null) {
            return false;
        }
        if (then == ClaimStatus.SETTLED) {
            return true;
        }
        if (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING) {
            return false;
        }
        return (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) || claim.bound()
                || holds.stream().noneMatch((h) -> h.covers(claim.date()));
    }

    /** Whether a claim dated {@code date} is after this person's retirement, and so refused. */
    public boolean retiredBefore(LocalDate date) {
        return retiredFrom != null && date.isAfter(LocalDate.parse(retiredFrom));
    }

    /** The grade in force on the date: the one whose from-date is the latest on or before it (null when none). */
    public String gradeOn(LocalDate date) {
        return gradeOn(grades, date);
    }

    /**
     * Whether the claim has a figure apart from the grade in force on {@code today}: one it keeps (settled or
     * bound), one at another grade, in force on its date, or one from a rate that ended (its to-date) on or before
     * today, the claim not being dated after today. Their due-total is n/a only when no claim does and that grade
     * has no rate on today.
     */
    public boolean figureApartFrom(Claim claim, LocalDate today, List<Rate> rates, Dated floor) {
        String then = gradeOn(claim.date());
        return claim.fixed()
                || (then != null && !then.equals(gradeOn(today)) && claim.figure(rates, then, floor) != null)
                || (then != null && !LocalDate.parse(claim.date()).isAfter(today)
                        && claim.figure(rates, then, floor) != null);
    }

    /**
     * The due-by of a claim of theirs dated {@code date}, fixed when it is made: the date plus the first of their own
     * terms, the terms of their grade then, and the sheet's terms then.
     */
    public LocalDate dueBy(String sheet, LocalDate date, Settings settings) {
        return date.plusDays(terms != null ? terms : settings.terms(sheet, gradeOn(date), date));
    }

    /** The grade in force on the claim's date (yyyy-mm-dd). */
    public String gradeOn(String date) {
        return gradeOn(LocalDate.parse(date));
    }

    static String gradeOn(List<GradeFrom> grades, LocalDate date) {
        GradeFrom found = null;
        for (GradeFrom g : grades) {
            if (g.from() != null && LocalDate.parse(g.from()).isAfter(date)) {
                continue;
            }
            if (found == null || (found.from() != null && g.from() != null && g.from().compareTo(found.from()) > 0)
                    || (found.from() == null && g.from() != null)) {
                found = g;
            }
        }
        return found == null ? null : found.grade();
    }
}
