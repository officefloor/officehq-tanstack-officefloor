package net.officefloor.hq.app.flag;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a reason that is not blank
 *  and an until date that is a real yyyy-mm-dd date. */
public class ValidateFlag {
    public void validate(@RequestBody FlagRequest request, Out<FlagInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before flagging people.");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidPersonException("Give a reason for the flag.");
        }
        String raw = request.until() == null ? "" : request.until().trim();
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidPersonException("The flag's until date must be a date (yyyy-mm-dd).");
        }
        LocalDate until;
        try {
            until = LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The flag's until date must be a real date (yyyy-mm-dd).");
        }
        body.set(new FlagInput(request.user().trim(), request.reason().trim(), until));
    }
}
