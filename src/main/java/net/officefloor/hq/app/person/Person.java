package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;

/**
 * A person on a sheet. {@code id} is null until the person is first saved; {@code version} counts its saves.
 * {@code statementCount} counts the statements taken; {@code statedTotal} is the last one's due-total
 * (n/a when it had none) and {@code statementDate} the sheet's today when it was taken, both null before the first.
 * {@code retiredFrom} is the date the person is retired from, or null when they are not retired. {@code held} is
 * whether the person is on hold: their open claims then count in no live total. {@code grades} are the grades
 * recorded for the person, each from its from-date; {@code grade} is the one in force on the sheet's today.
 */
public record Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
        String statedTotal, LocalDate statementDate, LocalDate retiredFrom, boolean held, List<GradeFrom> grades) {

    /** A person whose only grade applies from the beginning of time. */
    public Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
            String statedTotal, LocalDate statementDate, LocalDate retiredFrom, boolean held) {
        this(id, sheetId, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held,
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
                retiredFrom, held, grades);
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
                statedTotal, statementDate, retiredFrom, held, List.copyOf(next));
    }

    /** Whether the claim counts toward this person's live totals: a held person's open claims do not. */
    public boolean counts(Claim claim) {
        return claim.counts(held);
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
