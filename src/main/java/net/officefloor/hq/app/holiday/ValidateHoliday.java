package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the date is yyyy-MM-dd. */
public class ValidateHoliday {
    public void validate(@RequestBody HolidayRequest request, Out<HolidayValues> values) throws InvalidHolidayException {
        LocalDate date;
        try {
            date = LocalDate.parse(request == null || request.date() == null ? "" : request.date().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("A holiday must be a date yyyy-MM-dd");
        }
        values.set(new HolidayValues(date, request.by() == null ? "" : request.by().trim()));
    }
}
