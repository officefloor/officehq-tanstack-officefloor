package net.officefloor.hq.app.person;

/** A person on a sheet: the id the store assigned, their name and grade (A, B or C), and the
 *  version of these values (up by one on each save), and their statements: how many have been taken,
 *  and the last one's due-total and date (both null before the first), and the date they are retired
 *  from (null when not retired; any date set means retired at once), and whether they are on hold
 *  (their OPEN claims then count in no live total). */
public record Person(long id, String name, String grade, long version, long statementCount,
        String statedTotal, String statementDate, String retiredFrom, boolean held) {

    public boolean retired() {
        return retiredFrom != null;
    }
}
