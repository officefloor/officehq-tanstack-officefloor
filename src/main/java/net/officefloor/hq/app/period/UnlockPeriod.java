package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class UnlockPeriod {
    // Only a steward unlocks a locked period; it can then be reopened again.
    public void unlock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Settings settings, ServerHttpConnection connection, Out<Period> periodOut) throws PeriodRefusedException {
        periodOut.set(LockPeriod.change(sheetId, name, false, periods, settings, connection));
    }
}
