package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes HOLIDAY_ADDED to the audit file once the holiday is stored. */
public class RecordHolidayAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val HolidayValues values, Audit audit) {
        audit.record("HOLIDAY_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " date=" + values.date());
    }
}
