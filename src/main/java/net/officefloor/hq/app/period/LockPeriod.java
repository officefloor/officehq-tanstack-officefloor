package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Locks a period. Only a steward may lock it; locking a period that is already locked is refused. */
public class LockPeriod {
    public void lock(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods, RoleRepository roles)
            throws InvalidPeriodException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("Only a steward may lock a period");
        }
        if (!periods.setLocked(period, true)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already locked");
        }
    }
}
