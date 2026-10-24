package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the holiday's date from the body; refuses one that is not a calendar date. */
public class ValidateHoliday {
    public void validate(@RequestBody HolidayRequest request, Out<LocalDate> date) throws InvalidHolidayException {
        String value = request == null || request.date() == null ? "" : request.date().trim();
        try {
            date.set(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("The holiday must be a calendar date.");
        }
    }
}
