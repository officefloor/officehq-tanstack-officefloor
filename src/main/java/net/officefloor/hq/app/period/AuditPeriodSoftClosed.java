package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPeriodSoftClosed {
    public void audit(@Val Period period, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERIOD_SOFT_CLOSED", period.sheetId(), Audit.user(connection),
                "period=" + period.name(), "total=" + period.closedTotal().toPlainString()));
    }
}
