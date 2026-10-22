package net.officefloor.hq.app.claim;

import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and at least one selected claim. */
public class ValidateBulkSettle {
    public void validate(@RequestBody BulkSettleRequest request, Out<BulkSettleInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before settling claims.");
        }
        if (request.claimIds() == null || request.claimIds().isEmpty()) {
            throw new InvalidClaimException("Select the claims to settle.");
        }
        body.set(new BulkSettleInput(request.user().trim(), new LinkedHashSet<>(request.claimIds())));
    }
}
