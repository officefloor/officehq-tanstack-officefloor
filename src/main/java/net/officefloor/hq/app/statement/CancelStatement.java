package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cancels the person's last statement: the claims it bound are live again (a figure bound by a statement before it
 * is not restored), and the person shows one statement fewer, with the due-total and date of the one before it (or
 * none). Refused (400) when the user is not a steward or the person has no statement.
 */
public class CancelStatement {
    public void cancel(@Val Person person, @RequestParam("user") String user, SettingRepository settings,
            ClaimRepository claims, PersonRepository people, Out<CancelledStatement> cancelled, Out<Person> updated)
            throws InvalidStatementException {
        if (settings.find(person.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidStatementException("Only a steward may cancel a statement.");
        }
        if (person.statementCount() <= 0) {
            throw new InvalidStatementException(person.name() + " has no statement to cancel.");
        }
        cancelled.set(new CancelledStatement(person.name(), person.statedTotal()));
        claims.unbindStatement(person.sheetId(), person.id());
        updated.set(people.cancelStatement(person));
    }
}
