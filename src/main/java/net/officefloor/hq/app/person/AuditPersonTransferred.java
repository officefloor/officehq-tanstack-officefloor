package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonTransferred {
    // Each sheet records the transfer, then the ROLLUP and TOTALS lines for what moved on it.
    public void audit(@Val TransferredPerson transferred, @Val Figures before, Rollup rollup,
            ServerHttpConnection connection, Audit audit) {
        String user = Audit.user(connection);
        String from = transferred.from().sheetId();
        String to = transferred.toSheetId();
        String name = transferred.from().name();
        audit.record(Audit.line("PERSON_TRANSFERRED", from, user, "name=" + name, "to=" + to));
        RollupLines.record(from, user, before, rollup, audit);
        audit.record(Audit.line("PERSON_TRANSFERRED", to, user, "name=" + name, "from=" + from));
        RollupLines.record(to, user, transferred.toBefore(), rollup, audit);
    }
}
