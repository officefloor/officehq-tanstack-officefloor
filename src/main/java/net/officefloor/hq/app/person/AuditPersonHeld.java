package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonHeld {
    public void audit(@Val HoldDates dates, @Val Person loaded, ServerHttpConnection connection, Sheets sheets,
            Audit audit) {
        audit.record(Audit.line("PERSON_HELD", loaded.sheetId(), Audit.user(connection), "name=" + loaded.name(),
                "from=" + (dates.from() != null ? dates.from() : sheets.today(loaded.sheetId()))));
    }
}
