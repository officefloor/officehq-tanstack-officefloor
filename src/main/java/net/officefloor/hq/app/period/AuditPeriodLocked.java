package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPeriodLocked {
    public void audit(@Val Period period, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERIOD_LOCKED", period.sheetId(), Audit.user(connection),
                "period=" + period.name()));
    }
}
