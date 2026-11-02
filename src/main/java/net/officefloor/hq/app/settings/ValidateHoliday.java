package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the holiday form: a date (yyyy-MM-dd). */
public class ValidateHoliday {
    public void validate(@RequestBody HolidayRequest request, Out<LocalDate> holiday) throws BadRequestException {
        String date = request == null || request.date() == null ? "" : request.date().trim();
        if (date.isEmpty()) {
            throw new BadRequestException("date is required");
        }
        try {
            holiday.set(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("date must be yyyy-mm-dd");
        }
    }
}
