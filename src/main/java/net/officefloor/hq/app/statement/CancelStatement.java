package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward cancels a person's last statement: the claims it bound are unbound (live again — a figure a
 *  statement before it bound is not restored), the statement count goes down by one, and the person shows
 *  the due-total and date of the statement before it (or nothing). Refused for anyone but a steward, and
 *  for a person with no statement. */
public class CancelStatement {
    public void cancel(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository people, ClaimRepository claims,
            SettingsRepository settings, Audit audit, Rollup rollup, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = people.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can cancel a statement");
        }
        if (found.statementCount() <= 0) {
            throw new BadRequestException(found.name() + " has no statement to cancel");
        }
        Rollup.Snapshot before = rollup.take(id);
        for (Claim c : claims.ofPerson(id, pid)) {
            claims.bind(id, c.id(), false, null, false);
        }
        people.unstated(id, pid);
        audit.record("STATEMENT_CANCELLED sheet=" + id + " by=" + by + " name=" + found.name()
                + " due-total=" + found.statedTotal());
        rollup.record(id, by, before);
        person.set(people.find(id, pid));
    }
}
