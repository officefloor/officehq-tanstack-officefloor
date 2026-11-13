package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the holiday added to the sheet. */
public class AuditHolidayAdded {
    public void audit(@Val Holiday holiday, @RequestParam("user") String user, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.sheetId() + " by=" + user + " date=" + holiday.date());
    }
}
