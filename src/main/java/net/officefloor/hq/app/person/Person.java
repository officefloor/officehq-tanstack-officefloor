package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person on a sheet: a name (unique on the sheet, case ignored; kept without surrounding spaces) and a grade, one of A, B or C.
 *  The grade is dated: {@code grades} are the grades recorded, by from-date (the one with none, the first given, first), and
 *  {@code grade} is the one in force on the sheet's today.
 *  The version is 0 when added and goes up by one on each stored edit. {@code retiredFrom} (yyyy-MM-dd) is the date the
 *  person is retired from, null when not retired; a person with one counts as retired at once, whatever the date.
 *  {@code holds} are the person's holds, by from-date: an OPEN or PENDING claim of theirs dated within one, and not bound by
 *  a statement, counts in no live total. {@code held} is whether they have any hold. {@code terms} are the person's own
 *  terms (days a claim of theirs has until it is due by, ahead of their grade's and the sheet's), null when none. {@code contact} is the person's contact (unique on the sheet; kept without
 *  surrounding spaces, lower-cased), null when none. */
public record Person(long id, String sheetId, String name, String grade, long version, String retiredFrom, boolean held,
        List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact) {

    public Person(long id, String sheetId, String name, String grade, long version, String retiredFrom,
            List<PersonGrade> grades, List<PersonHold> holds, Integer terms, String contact) {
        this(id, sheetId, name, grade, version, retiredFrom, !holds.isEmpty(), grades, holds, terms, contact);
    }

    /** Whether a hold of the person covers the date. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch((h) -> h.covers(date));
    }

    /** Whether the person's retirement date is reached on the day: it is on or before it. From then on their OPEN or
     *  PENDING claims not bound by a statement count in no live total, as a hold's would. */
    public boolean retiredOn(LocalDate date) {
        return retiredFrom != null && !LocalDate.parse(retiredFrom).isAfter(date);
    }

    public boolean retired() {
        return retiredFrom != null;
    }

    /** Whether a claim of the person may be dated {@code date}: not after their retirement date. */
    public boolean mayClaimOn(java.time.LocalDate date) {
        return retiredFrom == null || !date.isAfter(java.time.LocalDate.parse(retiredFrom));
    }

    /** The grade in force on a date: the one with the latest from-date on or before it. */
    public String gradeOn(LocalDate date) {
        return gradeOn(grades, date);
    }

    /** The grade in force on a date among these, by from-date; null when there are none. */
    public static String gradeOn(List<PersonGrade> grades, LocalDate date) {
        PersonGrade found = null;
        for (PersonGrade g : grades) {
            if (g.from() != null && g.from().isAfter(date)) {
                continue;
            }
            if (found == null || found.from() == null || (g.from() != null && g.from().isAfter(found.from()))) {
                found = g;
            }
        }
        return found == null ? null : found.grade();
    }
}
