package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the retirement date from the body; refuses one that is not a calendar date. A blank date is left for
 *  the sheet's today. */
public class ValidateRetire {

    public record RetireRequest(String date) {
    }

    public void validate(@RequestBody(required = false) RetireRequest request, Out<RetireInput> body)
            throws InvalidPersonException {
        String date = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + date);
            }
        }
        body.set(new RetireInput(parsed));
    }
}
