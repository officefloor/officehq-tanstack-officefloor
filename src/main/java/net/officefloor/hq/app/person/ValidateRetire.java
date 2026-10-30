package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<ValidRetire> body) throws InvalidPersonException {
        String raw = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!raw.isEmpty()) {
            try {
                date = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The retirement date is not a date");
            }
        }
        body.set(new ValidRetire(date, request.user()));
    }
}
