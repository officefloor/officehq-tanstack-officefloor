package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and at least one selected claim. The void
 *  reason, chosen from the sheet's list, is kept (blank: none) and checked against the list for each claim. */
public class ValidateBulkVoid {
    public void validate(@RequestBody BulkVoidRequest request, Out<BulkVoidInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before voiding claims.");
        }
        if (request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select the claims to void.");
        }
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        body.set(new BulkVoidInput(request.user().trim(), new LinkedHashSet<>(request.claimIds()), reason));
    }
}
