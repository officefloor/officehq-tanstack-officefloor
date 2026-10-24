package net.officefloor.hq.app.statement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cancels the person's last statement: it is removed, and the claims it bound are live again (a figure bound by a
 * statement before it is not restored). Only a steward cancels, and a person with no statement has none to cancel.
 * Returns the cancelled statement, for the audit.
 */
public class CancelStatement {
    public void cancel(@Val Sheet sheet, @Val Person person, ClaimRepository claims, StatementRepository statements,
            RoleRepository roles, BindingLogRepository bindings, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<Statement> statementOut)
            throws ClerkLimitException, InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.getId(), AuditUser.decode(userHeader)))) {
            throw new ClerkLimitException("Only a steward can cancel a statement.");
        }
        // Lock the person so a cancel and a statement (or two cancels) are taken one after the other.
        entityManager.refresh(person, LockModeType.PESSIMISTIC_WRITE);
        List<Statement> taken = statements.ofPerson(person.getId());
        if (taken.isEmpty()) {
            throw new InvalidPersonException("The person has no statement to cancel.");
        }
        Statement last = taken.get(taken.size() - 1);
        // A statement binds or unbinds every claim of its person, so the claims bound now are the ones it bound.
        List<Claim> mine = claims.findBySheetIdAndPersonIdOrderByIdDesc(sheet.getId(), person.getId());
        for (Claim claim : mine) {
            claim.bind(false, null);
        }
        claims.saveAllAndFlush(mine);
        bindings.saveAndFlush(new BindingLog(sheet.getId(), person.getId(), sheet.getToday()));
        statements.delete(last);
        statements.flush();
        statementOut.set(last);
    }
}
