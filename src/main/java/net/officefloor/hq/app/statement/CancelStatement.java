package net.officefloor.hq.app.statement;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.StatedFigure;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Cancels the person's last statement. Only a steward may; a person with no statement is refused.
 * Every claim it bound is left unbound (live) — a figure an earlier statement bound is not restored.
 * The person's statement count goes down by one, and the statement before it (if any) is shown as
 * their last again.
 */
public class CancelStatement {
    public void cancel(@Val Sheet sheet, @Val Person person, ClaimRepository claims, PersonRepository people,
            RoleRepository roles, HttpServletRequest request, Out<CancelledStatement> cancelledOut)
            throws StewardOnlyException, NoStatementException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may cancel a statement");
        }
        int count = person.getStatementCount();
        if (count <= 0) throw new NoStatementException(person.getName() + " has no statement to cancel");
        String dueTotal = person.getStatedTotal();
        for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
            if (claim.isStated()) {
                claim.bind(false, null);
                claims.save(claim);
            }
        }
        StatedFigure before = count > 1 ? people.findStatement(person, count - 1) : null;
        person.setStatements(count - 1, before == null ? null : before.dueTotal(),
                before == null ? null : before.date());
        people.saveStatements(person);
        cancelledOut.set(new CancelledStatement(dueTotal));
    }
}
