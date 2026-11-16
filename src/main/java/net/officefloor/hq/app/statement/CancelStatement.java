package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Cancels the person's last statement: only a steward may, and a person with no statement is refused. The claims it
 * bound are unbound (live again); a figure an earlier statement bound is not restored.
 */
public class CancelStatement {
    public void cancel(@Val Person person, @Val StatementTaker taker, RoleRepository roles, StatementRepository statements,
            ClaimRepository claims, Out<Statement> cancelled) throws InvalidPersonException {
        String sheetId = person.sheetId();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheetId, taker.by()))) {
            throw new InvalidPersonException("Only a steward may cancel a statement");
        }
        Statement last = statements.last(sheetId, person.id());
        if (last == null || !statements.removeLast(sheetId, person.id())) {
            throw new InvalidPersonException(person.name() + " has no statement");
        }
        claims.unbindAll(sheetId, person.id());
        cancelled.set(new Statement(sheetId, person.id(), last.takenOn(), last.dueTotal(), last.claims(), taker.by(), last.asOf(), last.madeOn()));
    }
}
