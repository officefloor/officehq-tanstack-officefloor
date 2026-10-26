package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditHolidayAdded {
    public void audit(@Val Holiday holiday, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("HOLIDAY_ADDED", holiday.sheetId(), Audit.user(connection), "date=" + holiday.date()));
    }
}
