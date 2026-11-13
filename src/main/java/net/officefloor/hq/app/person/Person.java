package net.officefloor.hq.app.person;

import java.time.LocalDate;

/**
 * A person on a sheet. {@code id} is null until the person is first saved; {@code version} counts its saves.
 * {@code statementCount} counts the statements taken; {@code statedTotal} is the last one's due-total
 * (n/a when it had none) and {@code statementDate} the sheet's today when it was taken, both null before the first.
 * {@code retiredFrom} is the date the person is retired from, or null when they are not retired.
 */
public record Person(Long id, String sheetId, String name, String grade, int version, int statementCount,
        String statedTotal, LocalDate statementDate, LocalDate retiredFrom) {

    /** The same person with another name and grade. */
    public Person withNameAndGrade(String nextName, String nextGrade) {
        return new Person(id, sheetId, nextName, nextGrade, version, statementCount, statedTotal, statementDate,
                retiredFrom);
    }

    /** Whether the person is retired: a retirement date is set, whatever the date. */
    public boolean retired() {
        return retiredFrom != null;
    }

    /** Whether a claim on this date is refused: the person is retired from an earlier date. */
    public boolean retiredBefore(LocalDate date) {
        return retiredFrom != null && date.isAfter(retiredFrom);
    }
}
