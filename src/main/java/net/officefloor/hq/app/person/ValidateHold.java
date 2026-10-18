package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the hold body, refusing a from- or to-date that is not a date. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> holdOut) throws InvalidPersonException {
        holdOut.set(new HoldInput(date(request == null ? null : request.from(), "from"),
                date(request == null ? null : request.to(), "to")));
    }

    private static LocalDate date(String value, String which) throws InvalidPersonException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold " + which + "-date must be a date (yyyy-mm-dd).");
        }
    }
}
