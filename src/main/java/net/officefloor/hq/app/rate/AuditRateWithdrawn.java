package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditRateWithdrawn {
    public void audit(@Val WithdrawnRate withdrawn, ServerHttpConnection connection, Audit audit) {
        List<String> fields = new ArrayList<>(List.of("grade=" + withdrawn.grade()));
        if (withdrawn.from() != null) {
            fields.add("from=" + withdrawn.from());
        }
        audit.record(Audit.line("RATE_WITHDRAWN", withdrawn.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
