package net.officefloor.hq.app.person;

import java.util.List;

/** A person on a sheet: the id the store assigned, their name and the grade (one of the sheet's grades) in force on
 *  the sheet's today, with every grade they have been given (oldest first), and the
 *  version of these values (up by one on each save), and their statements: how many have been taken,
 *  and the last one's due-total and date (both null before the first), and the date they are retired
 *  from (null when not retired; any date set means retired at once), and their holds (oldest first) and
 *  whether they have any (their open (OPEN or PENDING) claims dated within a hold, unless bound by a
 *  statement, then count in no live total), and their own terms in days in force on the sheet's today (null when they have none), and their contact (null when they have none), and the sheet they were
 *  transferred from (null when they were not transferred here), and their past retirements (oldest first), and their tags (as {@link net.officefloor.hq.app.claim.Claim#tags(String)} keeps them; null when none),
 *  and their snapshots (one per SNAPSHOT line written for them, oldest first), and their statements still standing
 *  (by date, oldest first; those a merge or transfer brought over among them), and the reason of their flag while it is
 *  in force (the sheet's today on or before its until-date; null otherwise), and their approver (null when none). */
public record Person(long id, String name, String grade, long version, long statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held, List<GradeFrom> grades,
        List<Hold> holds, Integer terms, String contact, String origin,
        List<Retirement> retirements, String tags, List<SnapshotLine> snapshots, List<StatementLine> statements,
        String flag, String approver) {

    public Person withGrades(List<GradeFrom> grades) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements, tags, snapshots, statements, flag, approver);
    }

    public Person withHolds(List<Hold> holds) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom,
                !holds.isEmpty(), grades, holds, terms, contact, origin, retirements, tags, snapshots, statements, flag, approver);
    }

    public Person withRetirements(List<Retirement> retirements) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements, tags, snapshots, statements, flag, approver);
    }

    public Person withSnapshots(List<SnapshotLine> snapshots) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements, tags, snapshots, statements, flag, approver);
    }

    public Person withStatements(List<StatementLine> statements) {
        return new Person(id, name, grade, version, statementCount, statedTotal, statementDate, retiredFrom, held, grades, holds, terms, contact, origin, retirements, tags, snapshots, statements, flag, approver);
    }

    public boolean retired() {
        return retiredFrom != null;
    }

    /** The holds that act on live totals on the sheet's today: none of their own holds (a hold acts only on pool
     *  cuts), only, once their retirement date is reached (the sheet's today on or after it), a hold over every date. */
    public List<Hold> liveHolds(String today) {
        return claimedHolds(today);
    }

    /** The holds that keep their claims out of claimed on the sheet's today: none of their own holds (a hold
     *  leaves claimed alone), only, once their retirement date is reached, a hold over every date. */
    public List<Hold> claimedHolds(String today) {
        if (retiredFrom == null || today == null || retiredFrom.compareTo(today) > 0) return List.of();
        return List.of(new Hold(null, null));
    }
}
