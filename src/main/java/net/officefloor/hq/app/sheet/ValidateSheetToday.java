package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSheetToday {
    public void validate(@RequestBody SheetTodayRequest request, Out<LocalDate> today)
            throws InvalidSheetTodayException {
        String value = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetTodayException("Not a date: " + value);
        }
    }
}
