package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHoliday {
    public void validate(@RequestBody HolidayRequest request, Out<HolidayInput> body) throws InvalidHolidayException {
        String date = request == null || request.date() == null ? "" : request.date().trim();
        if (date.isEmpty()) throw new InvalidHolidayException("A holiday's date is required");
        try {
            body.set(new HolidayInput(LocalDate.parse(date)));
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("A date is yyyy-MM-dd");
        }
    }
}
