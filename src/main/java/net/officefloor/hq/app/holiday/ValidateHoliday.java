package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a real yyyy-mm-dd date. */
public class ValidateHoliday {
    public void validate(@RequestBody HolidayRequest request, Out<HolidayInput> body) throws InvalidHolidayException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidHolidayException("Sign in before adding a holiday.");
        }
        String raw = request.date() == null ? "" : request.date().trim();
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidHolidayException("The holiday must be a date (yyyy-mm-dd).");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("The holiday must be a real date (yyyy-mm-dd).");
        }
        body.set(new HolidayInput(request.user().trim(), date));
    }
}
