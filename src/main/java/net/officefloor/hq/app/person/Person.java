package net.officefloor.hq.app.person;

import java.util.List;

/** A person on a sheet: the id the store assigned, their name and the grade (A, B or C) in force on
 *  the sheet's today, with every grade they have been given (oldest first), and the
 *  version of these values (up by one on each save), and their statements: how many have been taken,
 *  and the last one's due-total and date (both null before the first), and the date they are retired
 *  from (null when not retired; any date set means retired at once), and their holds (oldest first) and
 *  whether they have any (their open (OPEN or PENDING) claims dated within a hold, unless bound by a
 *  statement, then count in no live total), and their own terms in days (null when they have none), and their contact (null when they have none), and the sheet they were
 *  transferred from (null when they were not transferred here), and their past retirements (oldest first). */
public record Person(long id, String name, String grade, long version, long statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held, List<GradeFrom> grades,
        List<Hold> holds, Integer terms, String contact, String origin,
        List<Retirement> retirements) {

    public Person withGrades(List<GradeFrom> grades) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements);
    }

    public Person withHolds(List<Hold> holds) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom,
                !holds.isEmpty(), grades, holds, terms, contact, origin, retirements);
    }

    public Person withRetirements(List<Retirement> retirements) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements);
    }

    public boolean retired() {
        return retiredFrom != null;
    }

    /** Their holds as they act on live totals on the sheet's today: once their retirement date is reached
     *  (the sheet's today on or after it) they are held over every date as well. */
    public List<Hold> liveHolds(String today) {
        if (retiredFrom == null || today == null || retiredFrom.compareTo(today) > 0) return holds;
        List<Hold> all = new java.util.ArrayList<>(holds);
        all.add(new Hold(null, null));
        return all;
    }
}
