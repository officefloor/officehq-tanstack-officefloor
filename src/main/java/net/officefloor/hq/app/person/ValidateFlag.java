package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the flag's reason (refused when blank) and until-date (blank: the sheet's today). */
public class ValidateFlag {

    public record FlagRequest(String reason, String until) {
    }

    public void validate(@RequestBody(required = false) FlagRequest request, Out<FlagInput> body)
            throws InvalidPersonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidPersonException("A flag needs a reason.");
        }
        LocalDate until = null;
        if (request.until() != null && !request.until().isBlank()) {
            try {
                until = LocalDate.parse(request.until().trim());
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The flag's until-date is not a date.");
            }
        }
        body.set(new FlagInput(reason, until));
    }
}
