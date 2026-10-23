package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and the claim's new due-by (yyyy-mm-dd). */
public class ValidateDueBySet {
    public void validate(@RequestBody DueBySetRequest request, Out<DueBySetInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        LocalDate dueBy;
        try {
            dueBy = LocalDate.parse(request.dueBy() == null ? "" : request.dueBy().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Give the claim's new due-by as yyyy-mm-dd.");
        }
        body.set(new DueBySetInput(request.user().trim(), dueBy));
    }
}
