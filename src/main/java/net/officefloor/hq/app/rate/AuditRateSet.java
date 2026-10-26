package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditRateSet {
    public void audit(@Val Rate rate, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("RATE_SET", rate.sheetId(), Audit.user(connection), "grade=" + rate.grade(),
                "rate=" + rate.rate().toPlainString()));
    }
}
