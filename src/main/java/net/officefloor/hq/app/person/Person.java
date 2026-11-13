package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.setting.DatedSettings;

/**
 * A person on a sheet. {@code id} is null until the person is first saved; {@code version} counts its saves.
 * {@code statementCount} counts the statements taken; {@code statedTotal} is the last one's due-total
 * (n/a when it had none) and {@code statementDate} the sheet's today when it was taken, both null before the first.
 * {@code retiredFrom} is the date the person is retired from, or null when they are not retired. {@code holds} are
 * the person's holds, oldest first: their open claims dated within one, and not bound by a statement, count in no
 * live total. {@code grades} are the grades
 * recorded for the person, each from its from-date; {@code grade} is the one in force on the sheet's today.
 * {@code terms} are the person's own terms in days, or null when they have none.
 */
public record Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
        String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds, List<GradeFrom> grades,
        Integer terms) {

    /** A person without terms of their own. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds,
            List<GradeFrom> grades) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                grades, null);
    }

    /** A person whose only grade applies from the beginning of time. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, List<Hold> holds) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, holds,
                List.of(new GradeFrom(grade, null)));
    }

    /** The grade in force on the date: the one recorded with the latest from-date on or before it. */
    public String gradeOn(LocalDate date) {
        return gradeOn(grades, date, grade);
    }

    static String gradeOn(List<GradeFrom> grades, LocalDate date, String otherwise) {
        return grades.stream().filter((g) -> g.from() == null || !g.from().isAfter(date))
                .max(Comparator.comparing(GradeFrom::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(GradeFrom::grade).orElse(otherwise);
    }

    /** The same person with another name. */
    public Person withName(String nextName) {
        return new Person(id, sheetId, nextName, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, terms);
    }

    /** The same person with these terms of their own (null: none). */
    public Person withTerms(Integer nextTerms) {
        return new Person(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate,
                retiredFrom, holds, grades, nextTerms);
    }

    /**
     * The terms a claim of the person's dated on the date is due by: their own, else those of their grade in force
     * on the date, else the sheet's in force on it.
     */
    public int termsOn(DatedSettings settings, LocalDate date) {
        return terms != null ? terms : settings.termsFor(gradeOn(date), date);
    }

    /**
     * The same person with another name and this grade recorded from the date (replacing any recorded from that
     * same date), keeping the others; {@code grade} becomes the one in force on {@code today}.
     */
    public Person withNameAndGrade(String nextName, String nextGrade, LocalDate from, LocalDate today) {
        List<GradeFrom> next = new ArrayList<>();
        for (GradeFrom g : grades) {
            if (g.from() == null ? from != null : !g.from().equals(from)) {
                next.add(g);
            }
        }
        next.add(new GradeFrom(nextGrade, from));
        return new Person(id, sheetId, nextName, gradeOn(next, today, nextGrade), version, statementCount,
                statedTotal, statementDate, retiredFrom, holds, List.copyOf(next), terms);
    }

    /** Whether the person has any hold, whatever its dates. */
    public boolean held() {
        return !holds.isEmpty();
    }

    /**
     * Whether the claim counts toward this person's live totals on the given today: a claim deferred then does not,
     * nor do their open claims dated within a hold, unless a statement binds them.
     */
    public boolean counts(Claim claim, LocalDate today) {
        return !claim.deferred(today) && claim.counts(!claim.bound() && heldOn(claim.date(), today));
    }

    /**
     * Whether the person's open claims on this date are held on the given today: a hold covers the date, or their
     * retirement date has been reached (then all of them are, as a hold's would be).
     */
    public boolean heldOn(LocalDate date, LocalDate today) {
        return retiredOn(today) || holds.stream().anyMatch((h) -> h.covers(date));
    }

    /** Whether the person's retirement date has been reached on the given today (on or after it). */
    public boolean retiredOn(LocalDate today) {
        return retiredFrom != null && today != null && !today.isBefore(retiredFrom);
    }

    /** Whether the person is retired: a retirement date is set, whatever the date. */
    public boolean retired() {
        return retiredFrom != null;
    }

    /** Whether a claim on this date is refused: the person is retired from an earlier date. */
    public boolean retiredBefore(LocalDate date) {
        return retiredFrom != null && date.isAfter(retiredFrom);
    }
}
