package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class ClosePeriod {
    // Only a role the permission matrix allows to close (by default a clerk or steward) closes a period. Fixes each person's due-total and carried figure for the period as it stands now, and the period's as their sum; stored only
    // if the period is still open.
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Rollup rollup, Settings settings, ServerHttpConnection connection, Out<Period> periodOut) throws PeriodRefusedException {
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        if (!settings.permits(period.sheetId(), "close", Audit.user(connection).trim())) {
            throw new PeriodRefusedException("Your role is not allowed to close a period.");
        }
        Period closed = period.closedAt(rollup.personDueTotals(period.sheetId(), period.from(), period.to()),
                rollup.personCarried(period.sheetId(), period, periods.list(period.sheetId())));
        if (period.closed() || !periods.close(closed)) {
            throw new PeriodRefusedException("The period " + period.name() + " is already closed.");
        }
        periodOut.set(closed);
    }
}
