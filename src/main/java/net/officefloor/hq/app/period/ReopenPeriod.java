package net.officefloor.hq.app.period;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Reopens a closed period, making its due-total live again. Only a steward may reopen a period,
 *  and only a closed one can be reopened. Publishes the reopened period. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @Val PeriodCloseRequest request, Periods periods, Settings settings,
            Out<ReopenedPeriod> reopened) throws PeriodClosedException {
        if (settings.role(period.sheetId(), request.user()) != Role.STEWARD) {
            throw new PeriodClosedException("Only a steward can reopen period " + period.name());
        }
        if (!period.closed()) throw new PeriodClosedException("Period " + period.name() + " is not closed");
        periods.reopen(period);
        reopened.set(new ReopenedPeriod(new Period(period.sheetId(), period.name(), period.from(), period.to())));
    }
}
