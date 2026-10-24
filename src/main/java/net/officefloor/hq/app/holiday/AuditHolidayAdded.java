package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the holiday, once it is added. */
public class AuditHolidayAdded {
    public void audit(@Val Holiday holiday, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.getSheetId() + " by=" + AuditUser.decode(user)
                + " date=" + holiday.getDate());
    }
}
