package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPeriodsGenerated {
    // Each generated period is recorded as an added one, in month order.
    public void audit(@Val GeneratedPeriods generated, ServerHttpConnection connection, Audit audit) {
        String user = Audit.user(connection);
        for (Period period : generated.periods()) {
            audit.record(Audit.line("PERIOD_ADDED", period.sheetId(), user, "period=" + period.name(),
                    "from=" + period.from(), "to=" + period.to()));
        }
    }
}
