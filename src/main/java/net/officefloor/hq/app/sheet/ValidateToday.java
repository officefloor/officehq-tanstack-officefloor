package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the new today from the body and refuses anything that is not a real ISO date. */
public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> todayOut) throws InvalidTodayException {
        String text = request == null || request.today() == null ? "" : request.today().trim();
        try {
            todayOut.set(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException("Today must be a date (yyyy-mm-dd).");
        }
    }
}
