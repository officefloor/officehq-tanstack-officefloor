package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the holiday to the sheet; refuses a date already on its list. Existing claims keep their due-by. */
public class ApplyHoliday {
    public void apply(@Val LocalDate date, @Val Sheet sheet, HolidayRepository repository, Out<Holiday> holidayOut)
            throws InvalidHolidayException {
        if (repository.existsBySheetIdAndDate(sheet.getId(), date)) {
            throw new InvalidHolidayException("That date is already a holiday.");
        }
        holidayOut.set(repository.saveAndFlush(new Holiday(sheet.getId(), date)));
    }
}
