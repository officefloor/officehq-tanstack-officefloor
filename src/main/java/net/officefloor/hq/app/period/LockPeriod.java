package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LockPeriod {
    // Only a steward locks a period that is not locked; a locked period cannot be reopened.
    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Settings settings, ServerHttpConnection connection, Out<Period> periodOut) throws PeriodRefusedException {
        periodOut.set(change(sheetId, name, true, periods, settings, connection));
    }

    static Period change(String sheetId, String name, boolean locked, Periods periods, Settings settings,
            ServerHttpConnection connection) throws PeriodRefusedException {
        String action = locked ? "lock" : "unlock";
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        if (!Settings.STEWARD.equals(settings.role(period.sheetId(), Audit.user(connection).trim()))) {
            throw new PeriodRefusedException("Only a steward may " + action + " a period.");
        }
        if (period.locked() == locked || !periods.lock(period, locked)) {
            throw new PeriodRefusedException("The period " + period.name() + " is already " + action + "ed.");
        }
        return period.withLocked(locked);
    }
}
