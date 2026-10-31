package net.officefloor.hq.app.person;

/** A person as the screen sees it, with their statement count, and the last statement's due-total
 *  (two decimals, null when it was n/a or none was taken) and date (yyyy-mm-dd, null when none was). */
public record PersonResponse(long id, String name, String grade, int version, int statementCount,
        String statedTotal, String statementDate) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                person.getStatementCount(),
                person.getStatedTotal() != null ? person.getStatedTotal().toPlainString() : null,
                person.getStatementDate() != null ? person.getStatementDate().toString() : null);
    }
}
