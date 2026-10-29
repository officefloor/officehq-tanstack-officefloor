package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonTransferUndone {
    // After the transfer's own lines, this sheet records the undo, then its ROLLUP and TOTALS lines for what moved on it.
    public void audit(@Val TransferredPerson transferred, @Val Figures before, Rollup rollup,
            ServerHttpConnection connection, Audit audit) {
        String user = Audit.user(connection);
        String sheet = transferred.from().sheetId();
        audit.record(Audit.line("PERSON_TRANSFER_UNDONE", sheet, user, "name=" + transferred.from().name(),
                "to=" + transferred.toSheetId()));
        RollupLines.record(sheet, user, before, rollup, audit);
    }
}
