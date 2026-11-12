package net.officefloor.hq.app.person;

import net.officefloor.hq.app.statement.StatementSummary;

/** A person as the front end reads it, with their statement count and the last statement's
 *  due-total and date (null when none was taken), whether they are retired and from when (null when not). */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom) {
    public static PersonResponse from(Person p, StatementSummary s) {
        return new PersonResponse(p.getId(), p.getName(), p.getGrade(), p.getVersion(), s.count(), s.lastDueTotal(),
                s.lastDate(), p.isRetired(),
                p.getRetiredFrom() == null ? null : p.getRetiredFrom().toString());
    }
}
