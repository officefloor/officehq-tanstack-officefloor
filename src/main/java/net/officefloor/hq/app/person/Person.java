package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A person on a sheet: a name and a grade (A, B or C: the one in force on the sheet's today), the version of the stored person, how many statements
 *  have been taken of them, and the last one's due-total (null when n/a, or none taken) and date (the sheet's
 *  today then; null when none taken), the date they are retired from (null: not retired), and whether they are
 *  dated grades and their dated holds (their OPEN claims dated within one, not bound by a statement, count in no
 *  live total), their own terms in days (null: none), their contact (trimmed, lower-cased; null: none), and their
 *  aliases, sorted as names are compared, the sheet they were last transferred from (null: never transferred), and
 *  their past retirements (each ended by unretiring them), in the order they ended, and their tags (null: none):
 *  trimmed, lower-cased, without repeats, sorted and joined by commas, and their statements still standing (not
 *  cancelled), in the order taken. */
public record Person(long id, String sheetId, String name, String grade, long version, int statementCount,
        BigDecimal statedTotal, LocalDate statementDate, LocalDate retiredFrom, Grades grades,
        Holds holds, Integer terms, String contact, List<String> aliases,
        String origin, List<Person.Retirement> retirements, String tags, List<Person.StatementLine> statements) {

    /** A statement still standing: its date and its due-total (null: n/a). */
    public record StatementLine(LocalDate date, BigDecimal dueTotal) {
    }

    /** A past retirement: from the date they were retired from to the sheet's today when they were unretired. */
    public record Retirement(LocalDate from, LocalDate to) {
        /** Whether a claim dated this day falls within it: after the from and before the to. */
        public boolean covers(LocalDate date) {
            return date.isAfter(from) && date.isBefore(to);
        }
    }
}
