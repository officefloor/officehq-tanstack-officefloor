package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body and parses the date strictly (2024-02-30 is refused). */
public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today) throws InvalidTodayException {
        String raw = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException("Not a date: " + raw);
        }
    }
}
