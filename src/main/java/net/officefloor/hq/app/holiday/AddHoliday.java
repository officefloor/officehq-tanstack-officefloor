package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a date to the sheet's holidays; refused for a date not valid or already on the list. */
public class AddHoliday {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody HolidayRequest request, Holidays holidays, Audit audit) throws InvalidHolidayException {
        String text = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("A holiday must be a date (yyyy-MM-dd)");
        }
        if (holidays.has(sheet, date)) {
            throw new InvalidHolidayException(date + " is already a holiday");
        }
        holidays.add(sheet, date);
        audit.record("HOLIDAY_ADDED sheet=" + sheet + " by=" + user + " date=" + date);
    }
}
