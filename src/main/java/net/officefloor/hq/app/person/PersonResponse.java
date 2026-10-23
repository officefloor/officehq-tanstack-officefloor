package net.officefloor.hq.app.person;

import java.util.List;

/** A person as the screen shows them, with their grade in force on the sheet's today, their statement count and
 *  the last statement's due-total (two decimals; null when n/a, or none taken) and date (yyyy-mm-dd; null when none
 *  taken), the date they are retired from (yyyy-mm-dd; null: not retired), whether they have any hold, their dated
 *  grades, earliest first (each from its from-date, yyyy-mm-dd; null: from the beginning of time), and their holds
 *  in the order they were put on (from- and to-date, yyyy-mm-dd; null: unbounded on that side), their own terms in days (null: none), their contact (null: none), their aliases, sorted as
 *  names are compared, whether their name or an alias is also used on another sheet of their sheet's tree, and the
 *  sheet they were last transferred from (null: never transferred), and their past retirements, in the order they
 *  ended, and their tags, sorted (empty when none), and their statements still standing, in the order taken. */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held, List<GradeResponse> grades,
        List<HoldResponse> holds, Integer terms, String contact, List<String> aliases,
        boolean clashing, String origin, List<RetirementResponse> retirements, List<String> tags,
        List<StatementLineResponse> statements) {

    /** A statement still standing: its date (yyyy-mm-dd) and due-total (two decimals; null: n/a). */
    public record StatementLineResponse(String date, String dueTotal) {
    }

    /** A past retirement from a from-date to the day they were unretired (yyyy-mm-dd). */
    public record RetirementResponse(String from, String to) {
    }

    /** A grade given from a from-date (yyyy-mm-dd; null: from the beginning of time). */
    public record GradeResponse(String grade, String from) {
    }

    /** A hold from a from-date to a to-date (yyyy-mm-dd; null: unbounded on that side). */
    public record HoldResponse(String from, String to) {
    }

    public static PersonResponse from(Person p) {
        return from(p, false);
    }

    public static PersonResponse from(Person p, boolean clashing) {
        return new PersonResponse(p.id(), p.name(), p.grade(), p.version(), p.statementCount(),
                p.statedTotal() != null ? p.statedTotal().toPlainString() : null,
                p.statementDate() != null ? p.statementDate().toString() : null,
                p.retiredFrom() != null ? p.retiredFrom().toString() : null, p.holds().any(),
                p.grades().history().stream()
                        .map(g -> new GradeResponse(g.grade(), g.from() != null ? g.from().toString() : null)).toList(),
                p.holds().history().stream().map(h -> new HoldResponse(h.from() != null ? h.from().toString() : null,
                        h.to() != null ? h.to().toString() : null)).toList(), p.terms(), p.contact(), p.aliases(),
                clashing, p.origin(), p.retirements().stream()
                        .map(r -> new RetirementResponse(r.from().toString(), r.to().toString())).toList(),
                p.tags() != null ? List.of(p.tags().split(",")) : List.of(),
                p.statements().stream().map(s -> new StatementLineResponse(
                        s.date() != null ? s.date().toString() : null,
                        s.dueTotal() != null ? s.dueTotal().toPlainString() : null)).toList());
    }
}
