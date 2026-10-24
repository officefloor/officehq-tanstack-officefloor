package net.officefloor.hq.app.role;

import java.time.LocalDate;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Delegates the steward role to the user name from the from-date to the to-date, inclusive (each blank: the sheet's
 *  today). Only a steward by their own role may (not one by delegation); refuses a to-date before the from-date. */
public class ApplyDelegation {
    public void apply(@Val DelegationInput input, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, RoleRepository roles,
            DelegationRepository repository, Out<Delegation> delegationOut) throws InvalidDelegationException {
        String by = AuditUser.decode(user);
        if (!Role.STEWARD.equals(roles.ownRoleOf(sheet.getId(), by))) {
            throw new InvalidDelegationException("Only a steward can delegate the steward role.");
        }
        LocalDate from = input.from() != null ? input.from() : sheet.getToday();
        LocalDate to = input.to() != null ? input.to() : sheet.getToday();
        if (to.isBefore(from)) {
            throw new InvalidDelegationException("The to-date must not be before the from-date.");
        }
        delegationOut.set(repository.saveAndFlush(new Delegation(sheet.getId(), input.user(), by, from, to)));
    }
}
