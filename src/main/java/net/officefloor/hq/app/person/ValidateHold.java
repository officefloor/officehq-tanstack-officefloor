package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the hold's from-date (blank: the sheet's today) and to-date (blank: open-ended). */
public class ValidateHold {

    public record HoldRequest(String from, String to) {
    }

    public void validate(@RequestBody(required = false) HoldRequest request, Out<HoldInput> body)
            throws InvalidPersonException {
        LocalDate from = request == null ? null : date(request.from(), "from");
        LocalDate to = request == null ? null : date(request.to(), "to");
        body.set(new HoldInput(from, to));
    }

    private static LocalDate date(String value, String what) throws InvalidPersonException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold's " + what + "-date is not a date.");
        }
    }
}
