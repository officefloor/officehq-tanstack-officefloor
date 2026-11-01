package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward cancels the person's last statement: the claims it bound are live again (none goes back to a figure an
 * earlier statement bound) and the statement before it, if any, is the last one shown. Refuses anyone else, or a
 * person with no statement.
 */
public class CancelStatement {
    public void cancel(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            Claims claims, Statements statements, Roles roles, Audit audit, Rollup rollup) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward cancels a statement");
        }
        Statements.Taken last = statements.last(sheet, person.id())
                .orElseThrow(() -> new InvalidPersonException(person.name() + " has no statement to cancel"));
        Rollup.Snapshot before = rollup.take(sheet);
        claims.unbindPerson(sheet, person.id());
        statements.delete(sheet, last.id());
        audit.record("STATEMENT_CANCELLED sheet=" + sheet + " by=" + user + " name=" + person.name() + " due-total="
                + (last.dueTotal() == null ? "n/a" : last.dueTotal().toPlainString()));
        rollup.record(sheet, user, before);
    }
}
