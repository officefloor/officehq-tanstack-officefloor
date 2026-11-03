package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Unlock a locked period, by a steward only: it can be reopened again. */
public class UnlockPeriod {
    public void unlock(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            ServerHttpConnection connection, PeriodRepository repository, SettingsRepository settings, Audit audit,
            Out<Period> period) throws BadRequestException {
        LockPeriod.apply(sheetId, periodName, false, connection, repository, settings, audit, period);
    }
}
