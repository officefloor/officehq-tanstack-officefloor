package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a holiday added: HOLIDAY_ADDED sheet=<id> by=<user> date=<date>. */
public class AuditHolidayAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val HolidayInput holiday, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + sheetId.trim() + " by=" + holiday.user() + " date=" + holiday.date());
    }
}
