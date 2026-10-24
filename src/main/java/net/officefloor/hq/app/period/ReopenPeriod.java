package net.officefloor.hq.app.period;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Reopens a closed period, so its due-total is live again. Only a steward may reopen one; refuses a period
 *  that is not closed. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @RequestHeader(name = "X-User", required = false) String user,
            PeriodRepository repository, RoleRepository roles) throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.roleOf(period.getSheetId(), AuditUser.decode(user)))) {
            throw new InvalidPeriodException("Only a steward can reopen a period.");
        }
        if (!period.isClosed()) {
            throw new InvalidPeriodException("The period is not closed.");
        }
        period.reopen();
        repository.saveAndFlush(period);
    }
}
