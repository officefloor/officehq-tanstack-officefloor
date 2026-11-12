package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.statement.StatementSummary;

/** A person as the front end reads it: the grade in force on the sheet's today and every grade with the
 *  date (yyyy-MM-dd) it applies from (null for the first, from the beginning of time), with their
 *  statement count and the last statement's due-total and date (null when none was taken), whether they
 *  are retired and from when (null when not), whether they are on hold, and the ids of their patrons. */
public record PersonResponse(long id, String name, String grade, List<GradeFrom> grades, long version,
        int statementCount, String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<Long> patronIds) {

    /** A grade and the date it applies from (null for from the beginning of time). */
    public record GradeFrom(String grade, String from) {
    }

    public static PersonResponse from(Person p, LocalDate today, StatementSummary s, List<Long> patronIds) {
        List<GradeFrom> grades = p.getGrades().entrySet().stream()
                .map(e -> new GradeFrom(e.getValue(), Person.START.equals(e.getKey()) ? null : e.getKey().toString()))
                .toList();
        return new PersonResponse(p.getId(), p.getName(), p.gradeOn(today), grades, p.getVersion(), s.count(),
                s.lastDueTotal(), s.lastDate(), p.isRetired(),
                p.getRetiredFrom() == null ? null : p.getRetiredFrom().toString(), p.isHeld(), patronIds);
    }
}
