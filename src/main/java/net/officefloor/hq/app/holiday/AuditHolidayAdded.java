package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored holiday in the audit file. */
public class AuditHolidayAdded {
    public void audit(@Val Holiday h, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + h.sheetId() + " by=" + h.by() + " date=" + h.date());
    }
}
