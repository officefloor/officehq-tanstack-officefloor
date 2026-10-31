package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the holiday is saved, so a refused one writes no audit line. */
public class RecordHolidayAdded {
    public void record(@Val Holiday holiday, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + holiday.sheetId() + " by=" + holiday.by() + " date=" + holiday.date());
    }
}
