package net.officefloor.hq.app.holiday;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the holiday added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditHolidayAdded {
    public void audit(@Val Holiday holiday, HttpServletRequest request, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.sheetId() + " by=" + Audit.user(request)
                + " date=" + holiday.date());
    }
}
