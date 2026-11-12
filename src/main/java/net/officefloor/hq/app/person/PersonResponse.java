package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.statement.StatementSummary;

/** A person as the front end reads it: the grade in force on the sheet's today and every grade with the
 *  date (yyyy-MM-dd) it applies from (null for the first, from the beginning of time), with their
 *  statement count and the last statement's due-total and date (null when none was taken), whether they
 *  are retired and from when (null when not) and whether the sheet's today has reached that date, whether they have any hold
 *  and each hold's from-date and to-date (null for no end), the ids of their patrons (in the order named), and their own terms
 *  in force on the sheet's today (whole days, null when none), their contact (null when none) and their aliases, sorted as names are compared, and whether they clash: their
 *  name or an alias is used as a name or alias on another sheet of the sheet's tree, and the sheet they were
 *  transferred from (null when they were not), and their past retirements, oldest first, and their tags, sorted, and their snapshots, oldest first, and their statements still standing, oldest first (by date). */
public record PersonResponse(long id, String name, String grade, List<GradeFrom> grades, long version,
        int statementCount, String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, List<HoldFrom> holds, List<Long> patronIds, Integer terms,
        boolean retiredNow, String contact, List<String> aliases, boolean clashing,
        String origin, List<RetirementFrom> pastRetirements, List<String> tags,
        List<SnapshotLine> snapshots, List<StatementSummary.Line> statements) {

    /** A snapshot's day being left (yyyy-MM-dd) and the due-total as it stood then (two decimals, or n/a). */
    public record SnapshotLine(String day, String dueTotal) {
    }

    /** A past retirement's from-date and to-date, the day they were unretired (yyyy-MM-dd). */
    public record RetirementFrom(String from, String to) {
    }

    /** A hold's from-date and to-date (yyyy-MM-dd; null for no end). */
    public record HoldFrom(String from, String to) {
    }

    /** A grade and the date it applies from (null for from the beginning of time). */
    public record GradeFrom(String grade, String from) {
    }

    public static PersonResponse from(Person p, LocalDate today, StatementSummary s, List<Long> patronIds,
            boolean clashing) {
        List<GradeFrom> grades = p.getGrades().entrySet().stream()
                .map(e -> new GradeFrom(e.getValue(), Person.START.equals(e.getKey()) ? null : e.getKey().toString()))
                .toList();
        return new PersonResponse(p.getId(), p.getName(), p.gradeOn(today), grades, p.getVersion(), s.count(),
                s.lastDueTotal(), s.lastDate(), p.isRetired(),
                p.getRetiredFrom() == null ? null : p.getRetiredFrom().toString(), p.isHeld(),
                p.getHolds().stream().map(h -> new HoldFrom(h.from().toString(), h.to() == null ? null : h.to().toString()))
                        .toList(),
                patronIds, p.termsOn(today), p.retiredOn(today), p.getContact(),
                List.copyOf(p.getAliases()), clashing, p.getOrigin(),
                p.getPastRetirements().stream().map(r -> new RetirementFrom(r.from().toString(), r.to().toString()))
                        .toList(),
                net.officefloor.hq.app.claim.Tags.list(p.getTags()),
                p.getSnapshots().stream().map(n -> new SnapshotLine(n.day().toString(), n.dueTotal())).toList(),
                s.lines());
    }
}
