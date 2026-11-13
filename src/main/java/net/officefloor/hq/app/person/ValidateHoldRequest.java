package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Refuses (400) a hold whose dates are not yyyy-mm-dd; a blank from-date is the sheet's today (null here), a blank
 * to-date no end.
 */
public class ValidateHoldRequest {
    public void validate(@RequestBody @Valid HoldRequest request, Out<Hold> hold) throws InvalidPersonException {
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        hold.set(new Hold(from, to));
    }

    private static LocalDate date(String value) throws InvalidPersonException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("A hold's dates must be yyyy-mm-dd.");
        }
    }
}
