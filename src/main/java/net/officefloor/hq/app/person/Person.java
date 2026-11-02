package net.officefloor.hq.app.person;

import java.util.List;

/** A person on a sheet: the id the store assigned, their name and the grade (A, B or C) in force on
 *  the sheet's today, with every grade they have been given (oldest first), and the
 *  version of these values (up by one on each save), and their statements: how many have been taken,
 *  and the last one's due-total and date (both null before the first), and the date they are retired
 *  from (null when not retired; any date set means retired at once), and their holds (oldest first) and
 *  whether they have any (their open (OPEN or PENDING) claims dated within a hold, unless bound by a
 *  statement, then count in no live total). */
public record Person(long id, String name, String grade, long version, long statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held, List<GradeFrom> grades,
        List<Hold> holds) {

    public Person withGrades(List<GradeFrom> grades) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds);
    }

    public Person withHolds(List<Hold> holds) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom,
                !holds.isEmpty(), grades, holds);
    }

    public boolean retired() {
        return retiredFrom != null;
    }
}
