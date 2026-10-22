package net.officefloor.hq.app.person;

import java.util.List;

/** A person as the screen shows them, with their grade in force on the sheet's today, their statement count and
 *  the last statement's due-total (two decimals; null when n/a, or none taken) and date (yyyy-mm-dd; null when none
 *  taken), the date they are retired from (yyyy-mm-dd; null: not retired), whether they are on hold, and their dated
 *  grades, earliest first (each from its from-date, yyyy-mm-dd; null: from the beginning of time). */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held, List<GradeResponse> grades) {

    /** A grade given from a from-date (yyyy-mm-dd; null: from the beginning of time). */
    public record GradeResponse(String grade, String from) {
    }

    public static PersonResponse from(Person p) {
        return new PersonResponse(p.id(), p.name(), p.grade(), p.version(), p.statementCount(),
                p.statedTotal() != null ? p.statedTotal().toPlainString() : null,
                p.statementDate() != null ? p.statementDate().toString() : null,
                p.retiredFrom() != null ? p.retiredFrom().toString() : null, p.held(),
                p.grades().history().stream()
                        .map(g -> new GradeResponse(g.grade(), g.from() != null ? g.from().toString() : null)).toList());
    }
}
