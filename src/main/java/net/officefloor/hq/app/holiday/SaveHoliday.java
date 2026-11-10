package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A date already on the sheet's list of holidays is refused. */
public class SaveHoliday {
    public void save(@Val HolidayInput input, @Val Sheet sheet, HolidayRepository repository,
            Out<Holiday> holidayOut) throws InvalidHolidayException {
        if (repository.exists(sheet.id(), input.date())) {
            throw new InvalidHolidayException(input.date() + " is already a holiday");
        }
        Holiday holiday = new Holiday(sheet.id(), input.date());
        repository.save(holiday);
        holidayOut.set(holiday);
    }
}
