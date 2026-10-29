package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class ReopenPeriod {
    // Only a role the permission matrix allows to reopen (by default a steward) reopens a closed or soft-closed period; its due-total, and its pool members' cuts, are then live again.
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            PoolCuts poolCuts, Settings settings, ServerHttpConnection connection, Out<Period> periodOut) throws PeriodRefusedException {
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        if (!settings.permits(period.sheetId(), "reopen", Audit.user(connection).trim())) {
            throw new PeriodRefusedException("Your role is not allowed to reopen a period.");
        }
        if (period.locked()) {
            throw new PeriodRefusedException("The period " + period.name() + " is locked.");
        }
        if (!(period.closed() || period.soft()) || !periods.reopen(period)) {
            throw new PeriodRefusedException("The period " + period.name() + " is not closed.");
        }
        poolCuts.drop(period.sheetId(), period.name());
        periodOut.set(period.reopened());
    }
}
