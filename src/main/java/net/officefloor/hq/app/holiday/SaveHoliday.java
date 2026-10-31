package net.officefloor.hq.app.holiday;

import net.officefloor.plugin.variable.Val;

public class SaveHoliday {
    public void save(@Val Holiday holiday, Holidays holidays) {
        holidays.add(holiday.sheetId(), holiday.date());
    }
}
