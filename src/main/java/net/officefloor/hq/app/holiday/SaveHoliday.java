package net.officefloor.hq.app.holiday;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val HolidayInput holiday, HolidayStore store) {
        store.add(sheetId.trim(), holiday.date());
    }
}
