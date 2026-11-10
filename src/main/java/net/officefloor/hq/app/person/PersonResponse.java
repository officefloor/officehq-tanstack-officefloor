package net.officefloor.hq.app.person;

/**
 * A person, as the screen reads it, with their statement count, and the last statement's due-total
 * and date ("" before the first).
 */
public record PersonResponse(String id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(),
                person.getVersion(), person.getStatementCount(),
                person.getStatedTotal() == null ? "" : person.getStatedTotal(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString());
    }
}
