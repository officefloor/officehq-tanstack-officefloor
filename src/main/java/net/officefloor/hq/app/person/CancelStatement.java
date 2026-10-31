package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward cancels the person's last statement. The claims it bound (those bound now: each
 *  statement rebinds or unbinds every claim of the person) become live again — a figure an earlier
 *  statement bound is not restored. The count goes down by one and the person shows the statement
 *  before it, or none. Refused: anyone but a steward, a person with no statement. Read afresh and
 *  locked, so it happens one after another with a statement taken at once. */
public class CancelStatement {
    public void cancel(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val StatementRequest request, People people, Claims claims, Settings settings,
            Out<Person> personOut, Out<Statement> statement)
            throws PersonNotFoundException, InvalidSheetRequestException {
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can cancel a statement");
        }
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        if (person.getStatementCount() <= 0) {
            throw new InvalidSheetRequestException("This person has no statement to cancel");
        }
        Statement cancelled = new Statement(person.getStatedTotal(), 0, request.user());
        claims.unbind(sheetId, personId);
        people.cancelStatement(person);
        personOut.set(person);
        statement.set(cancelled);
    }
}
