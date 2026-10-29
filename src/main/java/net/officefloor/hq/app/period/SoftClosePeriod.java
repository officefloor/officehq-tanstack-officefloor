package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SoftClosePeriod {
    // Only a role the permission matrix allows to close soft-closes a period. Fixes each person's due-total and carried figure for the open period as it
    // stands now, and the period's as their sum, and every pool member's cut for it as it stands now, while its claims stay open to change; stored only if the period is still open.
    public void softClose(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Rollup rollup, PoolCuts poolCuts, Settings settings, ServerHttpConnection connection, Out<Period> periodOut) throws PeriodRefusedException {
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        if (!settings.permits(period.sheetId(), "close", Audit.user(connection).trim())) {
            throw new PeriodRefusedException("Your role is not allowed to close a period.");
        }
        Period soft = period.softClosedAt(rollup.personDueTotals(period.sheetId(), period.from(), period.to()),
                rollup.personCarried(period.sheetId(), period, periods.list(period.sheetId())));
        if (period.closed() || period.soft() || !periods.softClose(soft)) {
            throw new PeriodRefusedException("The period " + period.name() + " is already closed.");
        }
        poolCuts.fix(soft.sheetId(), soft.name());
        periodOut.set(soft);
    }
}
