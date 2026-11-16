package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: each date is blank or yyyy-MM-dd. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<Hold> hold) throws InvalidPersonException {
        LocalDate from = date(request == null ? null : request.from(), "from");
        LocalDate to = date(request == null ? null : request.to(), "to");
        hold.set(new Hold(from, to, request == null || request.by() == null ? "" : request.by().trim()));
    }

    private static LocalDate date(String value, String which) throws InvalidPersonException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold's " + which + "-date must be yyyy-MM-dd");
        }
    }
}
