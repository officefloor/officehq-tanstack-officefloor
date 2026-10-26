package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditRateSet {
    public void audit(@Val Rate rate, ServerHttpConnection connection, Audit audit) {
        List<String> fields = new ArrayList<>(List.of("grade=" + rate.grade(), "rate=" + rate.rate().toPlainString()));
        if (rate.from() != null) {
            fields.add("from=" + rate.from());
        }
        if (rate.to() != null) {
            fields.add("to=" + rate.to());
        }
        audit.record(Audit.line("RATE_SET", rate.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
