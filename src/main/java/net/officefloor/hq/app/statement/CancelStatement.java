package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Statements;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Cancels the person's last statement; only a steward may, and a person with no statement is refused. The claims it
 * bound are live again (a figure bound by a statement before it is not restored), and the statement before it is
 * the person's last.
 */
public class CancelStatement {
    public void cancel(@Val Person person, @Val ChangedBy by, RoleRepository roles, StatementRepository statements,
            ClaimRepository claims, Out<CancelledStatement> cancelled) throws BadRequestException {
        String sheetId = person.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by.name()))) {
            throw new BadRequestException("Only a steward may cancel a statement");
        }
        StatementRepository.Taken last = statements.last(sheetId, person.getId());
        if (last == null) {
            throw new BadRequestException("Nobody has taken a statement of " + person.getName());
        }
        if (!statements.delete(last.id())) {
            throw new BadRequestException("The statement was cancelled by someone else; reload the sheet");
        }
        claims.unbindAllOf(sheetId, person.getId());
        StatementRepository.Taken before = statements.last(sheetId, person.getId());
        person.setStatements(before == null ? Statements.NONE
                : new Statements(person.getStatements().count() - 1, before.dueTotal(), before.takenOn()));
        cancelled.set(new CancelledStatement(last.dueTotal()));
    }
}
