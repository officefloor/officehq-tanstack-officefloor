package net.officefloor.hq.app.period;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Unlocks the period, so it can be reopened again. Only a steward may unlock a period, and only a
 *  locked one. Publishes the unlocked period. */
public class UnlockPeriod {
    public void unlock(@Val Period period, @Val PeriodCloseRequest request, Periods periods, Settings settings,
            Out<UnlockedPeriod> unlocked) throws PeriodClosedException {
        if (settings.role(period.sheetId(), request.user()) != Role.STEWARD) {
            throw new PeriodClosedException("Only a steward may unlock period " + period.name());
        }
        if (!period.locked()) throw new PeriodClosedException("Period " + period.name() + " is not locked");
        periods.setLocked(period, false);
        unlocked.set(new UnlockedPeriod(period.withLocked(false)));
    }
}
