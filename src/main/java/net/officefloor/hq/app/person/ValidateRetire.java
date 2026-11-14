package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the retirement date: null when blank (the sheet's today); a date that is not a date is refused. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireRequest> body)
            throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + from);
            }
        }
        body.set(new RetireRequest(from.isEmpty() ? null : from));
    }
}
