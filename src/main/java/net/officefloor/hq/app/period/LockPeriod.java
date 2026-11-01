package net.officefloor.hq.app.period;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Locks the period, so it cannot be reopened. Only a steward may lock a period, and only one not
 *  already locked. Publishes the locked period. */
public class LockPeriod {
    public void lock(@Val Period period, @Val PeriodCloseRequest request, Periods periods, Settings settings,
            Out<LockedPeriod> locked) throws PeriodClosedException {
        if (settings.role(period.sheetId(), request.user()) != Role.STEWARD) {
            throw new PeriodClosedException("Only a steward may lock period " + period.name());
        }
        if (period.locked()) throw new PeriodClosedException("Period " + period.name() + " is already locked");
        periods.setLocked(period, true);
        locked.set(new LockedPeriod(period.withLocked(true)));
    }
}
