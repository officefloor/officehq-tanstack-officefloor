package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body)
            throws InvalidSheetException {
        String text = request == null || request.today() == null ? "" : request.today().trim();
        body.set(request);
        try {
            today.set(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
