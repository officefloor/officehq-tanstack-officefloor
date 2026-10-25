package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordHolidayAdded {
    public void record(@Val Holiday holiday, @Val ChangedBy by, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.sheetId() + " by=" + by.name() + " date=" + holiday.date());
    }
}
