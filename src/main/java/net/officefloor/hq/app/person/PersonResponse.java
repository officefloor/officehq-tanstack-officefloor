package net.officefloor.hq.app.person;

/** A person as the screen sees it, with their statement count, and the last statement's due-total
 *  (two decimals, null when it was n/a or none was taken) and date (yyyy-mm-dd, null when none was),
 *  and whether they are retired and from when (yyyy-mm-dd, null when not retired),
 *  and whether they are on hold. */
public record PersonResponse(long id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() != null ? person.getStatedTotal().toPlainString() : null,
                person.getStatementDate() != null ? person.getStatementDate().toString() : null,
                person.isRetired(), person.getRetiredFrom() != null ? person.getRetiredFrom().toString() : null,
                person.isHeld());
    }
}
