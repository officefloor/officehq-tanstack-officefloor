package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditRateSet {
    public void audit(@Val Rate rate, ServerHttpConnection connection, Audit audit) {
        String[] fields = rate.from() == null
                ? new String[] { "grade=" + rate.grade(), "rate=" + rate.rate().toPlainString() }
                : new String[] { "grade=" + rate.grade(), "rate=" + rate.rate().toPlainString(), "from=" + rate.from() };
        audit.record(Audit.line("RATE_SET", rate.sheetId(), Audit.user(connection), fields));
    }
}
