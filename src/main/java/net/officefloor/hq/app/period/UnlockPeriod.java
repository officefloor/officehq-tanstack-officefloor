package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;

/** Unlocks a period. Only a steward may unlock it; unlocking a period that is already unlocked is refused. */
public class UnlockPeriod {
    public void unlock(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods, RoleRepository roles)
            throws InvalidPeriodException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("Only a steward may unlock a period");
        }
        if (!periods.setLocked(period, false)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already unlocked");
        }
    }
}
