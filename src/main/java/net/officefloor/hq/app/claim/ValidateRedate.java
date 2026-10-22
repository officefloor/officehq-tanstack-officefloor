package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and the claim's new date (yyyy-mm-dd). */
public class ValidateRedate {
    public void validate(@RequestBody RedateRequest request, Out<RedateInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.date() == null ? "" : request.date().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Give the claim's new date as yyyy-mm-dd.");
        }
        body.set(new RedateInput(request.user().trim(), date));
    }
}
