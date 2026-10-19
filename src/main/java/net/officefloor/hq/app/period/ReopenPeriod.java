package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Reopens a closed period, so its due-total is live again. Only a steward may reopen; a period that
 *  is not closed cannot be reopened. */
public class ReopenPeriod {
    public void reopen(@Val Period period, RoleRepository roles, PeriodRepository repository,
            @RequestParam(value = "user", required = false) String user, Out<ReopenedPeriod> reopenedOut)
            throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.roleOf(period.sheetId(), user))) {
            throw new InvalidPeriodException("Only a steward can reopen a period.");
        }
        if (!period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed.");
        }
        Period reopened = period.reopen();
        repository.saveClosed(reopened);
        reopenedOut.set(new ReopenedPeriod(reopened));
    }
}
