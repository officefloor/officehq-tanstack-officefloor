package net.officefloor.hq.app.period;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPeriodAdded {
    public void audit(@Val Period period, ServerHttpConnection connection, Audit audit) {
        List<String> fields = new ArrayList<>(List.of("period=" + period.name(), "from=" + period.from(), "to=" + period.to()));
        if (period.cap() != null) {
            fields.add("cap=" + period.cap());
        }
        audit.record(Audit.line("PERIOD_ADDED", period.sheetId(), Audit.user(connection), fields.toArray(String[]::new)));
    }
}
