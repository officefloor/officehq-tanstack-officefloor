package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<ValidHold> body) throws InvalidPersonException {
        body.set(new ValidHold(date(request.from(), "from"), date(request.to(), "to"), request.user()));
    }

    private static LocalDate date(String value, String which) throws InvalidPersonException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold's " + which + "-date is not a date");
        }
    }
}
