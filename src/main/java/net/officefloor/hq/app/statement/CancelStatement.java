package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Cancel the person's last statement: only a steward may, and only of a person with a statement. The claims it
 *  bound become live again (a figure an earlier statement bound them to is not restored), there is one statement
 *  fewer, and the row shows the one before it (or none). Publishes the cancelled statement (its due-total; the
 *  claims it bound). */
public class CancelStatement {
    public void cancel(@Val StatementRequest request, @Val Person loaded, PersonRepository people,
            ClaimRepository claims, RoleRepository roles, Out<Statement> statement, Out<PersonResponse> saved)
            throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), request.user()))) {
            throw new InvalidPersonException("Only a steward may cancel a statement.");
        }
        people.lock(loaded);
        Person person = people.find(loaded.sheetId(), loaded.id());
        if (person.statementCount() == 0) {
            throw new InvalidPersonException(person.name() + " has no statement to cancel.");
        }
        int bound = 0;
        for (Claim c : claims.listForPerson(person.sheetId(), person.id())) {
            if (c.stated()) {
                bound++;
                claims.state(c, false, null);
            }
        }
        people.unstated(person);
        statement.set(new Statement(person.sheetId(), person.name(), person.statedTotal(), bound));
        saved.set(PersonResponse.from(people.find(person.sheetId(), person.id())));
    }
}
