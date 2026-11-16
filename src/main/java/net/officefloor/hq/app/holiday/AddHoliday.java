package net.officefloor.hq.app.holiday;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the holiday on the sheet; a date already on its list is refused. */
public class AddHoliday {
    public void add(@PathVariable("sheetId") String sheetId, @Val HolidayValues values, HolidayRepository holidays)
            throws InvalidHolidayException {
        if (!holidays.add(sheetId.trim(), values.date())) {
            throw new InvalidHolidayException(values.date() + " is already a holiday");
        }
    }
}
