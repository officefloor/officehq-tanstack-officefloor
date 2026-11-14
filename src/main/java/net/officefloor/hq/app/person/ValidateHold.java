package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the hold's dates: null when blank; a date that is not a date, or a to-date before the
 *  from-date, is refused. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldRequest> body)
            throws InvalidPersonException {
        String from = date(request == null ? null : request.from());
        String to = date(request == null ? null : request.to());
        if (from != null && to != null && to.compareTo(from) < 0) {
            throw new InvalidPersonException("The hold ends before it starts: " + from + " to " + to);
        }
        body.set(new HoldRequest(from, to));
    }

    private static String date(String text) throws InvalidPersonException {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a date: " + value);
        }
    }
}
