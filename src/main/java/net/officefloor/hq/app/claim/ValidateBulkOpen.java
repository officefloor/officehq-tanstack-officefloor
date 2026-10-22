package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and at least one selected claim. */
public class ValidateBulkOpen {
    public void validate(@RequestBody BulkOpenRequest request, Out<BulkOpenInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before opening claims.");
        }
        if (request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select the claims to open.");
        }
        body.set(new BulkOpenInput(request.user().trim(), new LinkedHashSet<>(request.claimIds())));
    }
}
