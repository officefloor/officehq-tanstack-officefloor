package net.officefloor.hq.app.person;

/** A person, with their statement count and their last statement's due-total ("n/a" when it had none) and date
 *  (both null before their first statement). */
public record PersonResponse(long id, String name, String grade, long version, int statementCount, String statedTotal,
        String statementDate) {
    public static PersonResponse from(Person person) {
        Statements s = person.getStatements();
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(), s.count(),
                s.count() == 0 ? null : s.lastDueTotal() == null ? "n/a" : s.lastDueTotal().toPlainString(),
                s.lastDate() == null ? null : s.lastDate().toString());
    }
}
