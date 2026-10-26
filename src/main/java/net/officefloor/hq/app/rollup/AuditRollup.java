package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AuditRollup {
    // After the change's own lines: one ROLLUP per person whose figures moved (a new person counts as moved),
    // in the order they were added, then TOTALS when a sheet total moved.
    public void audit(@PathVariable("sheetId") String sheetId, @Val Figures before, Rollup rollup,
            ServerHttpConnection connection, Audit audit) {
        RollupLines.record(sheetId.trim(), Audit.user(connection), before, rollup, audit);
    }
}
