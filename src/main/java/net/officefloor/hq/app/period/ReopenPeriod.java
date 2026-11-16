package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Reopens a closed period, so its due-total is live again. Only a steward may reopen; reopening a period that is
 *  not closed is refused. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods,
            RoleRepository roles) throws InvalidPeriodException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("Only a steward may reopen a period");
        }
        if (!period.closed() || !periods.reopen(period)) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed");
        }
    }
}
