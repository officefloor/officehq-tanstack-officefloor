package net.officefloor.hq.app.person;

/**
 * A person, as the screen reads it, with their statement count, and the last statement's due-total
 * and date ("" before the first), whether they are retired and the date they are retired from
 * ("" when not), whether they are on hold, and the ids of the people they name as patrons.
 */
public record PersonResponse(String id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, boolean retired, String retiredFrom,
        boolean held, java.util.List<String> patronIds) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(String.valueOf(person.getId()), person.getName(), person.getGrade(),
                person.getVersion(), person.getStatementCount(),
                person.getStatedTotal() == null ? "" : person.getStatedTotal(),
                person.getStatementDate() == null ? "" : person.getStatementDate().toString(),
                person.isRetired(), person.getRetiredFrom() == null ? "" : person.getRetiredFrom().toString(),
                person.isHeld(), person.getPatronIds().stream().map(String::valueOf).toList());
    }
}
