package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the new today from the body; refuses anything that is not a calendar date. */
public class ValidateToday {

    public record TodayRequest(String today) {
    }

    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today) throws InvalidTodayException {
        String raw = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException(raw);
        }
    }
}
