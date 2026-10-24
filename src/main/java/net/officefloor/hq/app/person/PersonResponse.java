package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.statement.Statement;

/** A person as the screen shows them, with their statement count and their last statement's due-total and
 *  date (null with none), and the date they are retired from (null: not retired). */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate, String retiredFrom) {

    /** @param statements the person's statements, oldest first. */
    public static PersonResponse from(Person person, List<Statement> statements) {
        Statement last = statements.isEmpty() ? null : statements.get(statements.size() - 1);
        return new PersonResponse(person.getId(), person.getName(), person.getGrade(), person.getVersion(),
                statements.size(), last == null ? null : last.getDueTotal(),
                last == null ? null : last.getTakenOn().toString(),
                person.getRetiredFrom() == null ? null : person.getRetiredFrom().toString());
    }
}
