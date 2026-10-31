package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a holiday that is not a date, or a date already on the sheet's list. */
public class ValidateHoliday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HolidayRequest request,
            Holidays holidays, Out<Holiday> holiday, Out<Actor> actor) throws InvalidSheetRequestException {
        String raw = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
        if (holidays.has(sheetId, date)) {
            throw new InvalidSheetRequestException("Already a holiday: " + date);
        }
        holiday.set(new Holiday(sheetId, date, request.user()));
        actor.set(new Actor(request.user()));
    }
}
