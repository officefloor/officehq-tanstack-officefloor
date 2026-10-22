package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person on a sheet: a name and a grade (A, B or C), the version of the stored person, how many statements
 *  have been taken of them, and the last one's due-total (null when n/a, or none taken) and date (the sheet's
 *  today then; null when none taken), and the date they are retired from (null: not retired). */
public record Person(long id, String sheetId, String name, String grade, long version, int statementCount,
        BigDecimal statedTotal, LocalDate statementDate, LocalDate retiredFrom) {
}
