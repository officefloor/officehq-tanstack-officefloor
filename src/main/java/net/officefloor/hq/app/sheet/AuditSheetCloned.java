package net.officefloor.hq.app.sheet;

import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSheetCloned {
    // This sheet records the clone made; the clone starts with SHEET_CLONED, then a ROLLUP line for every person and its TOTALS.
    public void audit(@Val ClonedSheet cloned, Rollup rollup, ServerHttpConnection connection, Audit audit) {
        String user = Audit.user(connection);
        audit.record(Audit.line("SHEET_CLONE_MADE", cloned.fromSheetId(), user, "to=" + cloned.toSheetId()));
        audit.record(Audit.line("SHEET_CLONED", cloned.toSheetId(), user, "from=" + cloned.fromSheetId()));
        // Figures matching nothing, so every line is recorded.
        RollupLines.record(cloned.toSheetId(), user, new Figures(Map.of(), "", "", ""), rollup, audit);
    }
}
