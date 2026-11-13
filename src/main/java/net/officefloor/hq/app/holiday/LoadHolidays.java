package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadHolidays {
    public void load(@Val Sheet sheet, HolidayRepository repository, Out<Holidays> holidays) {
        holidays.set(new Holidays(repository.findAll(sheet.id())));
    }
}
