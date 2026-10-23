package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, at least one selected claim and their new
 *  date (yyyy-mm-dd). */
public class ValidateBulkRedate {
    public void validate(@RequestBody BulkRedateRequest request, Out<BulkRedateInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        if (request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select the claims to re-date.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(request.date() == null ? "" : request.date().trim());
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Give the claims' new date as yyyy-mm-dd.");
        }
        body.set(new BulkRedateInput(request.user().trim(), new LinkedHashSet<>(request.claimIds()), date));
    }
}
