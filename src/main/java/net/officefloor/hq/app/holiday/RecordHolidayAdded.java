package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a holiday that went through is recorded. */
public class RecordHolidayAdded {
    public void record(@Val Holiday holiday, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.sheetId() + " by=" + holiday.by() + " date=" + holiday.date());
    }
}
