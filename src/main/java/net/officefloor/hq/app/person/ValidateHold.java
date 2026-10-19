package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the hold request, refusing a date that is not one; a blank from-date stays empty here, to
 *  default to the sheet's today, and a blank to-date leaves the hold open-ended. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldRequest> hold) throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().strip();
        String to = request == null || request.to() == null ? "" : request.to().strip();
        try {
            if (!from.isEmpty()) LocalDate.parse(from);
            if (!to.isEmpty()) LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date.");
        }
        hold.set(new HoldRequest(from, to));
    }
}
