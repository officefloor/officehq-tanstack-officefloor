package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AuditQuickRollup {
    // What the claim moved, compared with the figures once its person was in place.
    public void audit(@PathVariable("sheetId") String sheetId, @Val QuickFigures before, Rollup rollup,
            ServerHttpConnection connection, Audit audit) {
        RollupLines.record(sheetId.trim(), Audit.user(connection), before.figures(), rollup, audit);
    }
}
