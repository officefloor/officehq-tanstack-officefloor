package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body)
            throws InvalidTodayException {
        body.set(request); // who changes it, for the audit
        String raw = request.today() == null ? "" : request.today().trim();
        if (raw.isEmpty()) {
            today.set(LocalDate.now()); // blank takes the default: the real date
            return;
        }
        try {
            today.set(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException(raw);
        }
    }
}
