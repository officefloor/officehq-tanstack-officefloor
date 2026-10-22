package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateToday {
    public void validate(@RequestBody TodayRequest request, Out<LocalDate> today, Out<TodayRequest> body) throws InvalidSheetException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in before changing the sheet's today.");
        }
        String raw = request.today() == null ? "" : request.today().trim();
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidSheetException("Today must be a date (yyyy-mm-dd).");
        }
        try {
            today.set(LocalDate.parse(raw));
            body.set(new TodayRequest(request.user().trim(), raw));
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Today must be a real date (yyyy-mm-dd).");
        }
    }
}
