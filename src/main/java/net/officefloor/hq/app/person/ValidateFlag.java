package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the flag: its reason trimmed (blank is refused) and its until-date (null when blank; a date
 *  that is not a date is refused). */
public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<FlagRequest> body)
            throws InvalidPersonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason");
        }
        String until = request.until() == null ? "" : request.until().trim();
        if (!until.isEmpty()) {
            try {
                until = LocalDate.parse(until).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + until);
            }
        }
        body.set(new FlagRequest(reason, until.isEmpty() ? null : until));
    }
}
