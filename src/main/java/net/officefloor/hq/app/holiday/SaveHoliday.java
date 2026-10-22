package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the holiday, refusing a date already on the sheet's list. Existing claims keep their due-by. */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val HolidayInput input, PeriodRepository periods,
            HolidayRepository repository, Out<Holiday> holiday) throws InvalidHolidayException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (repository.exists(id, input.date())) {
            throw new InvalidHolidayException(input.date() + " is already a holiday.");
        }
        holiday.set(repository.add(id, input));
    }
}
