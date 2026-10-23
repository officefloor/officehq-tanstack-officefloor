package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user. */
public class ValidateClaimHold {
    public void validate(@RequestBody ClaimHoldRequest request, Out<ClaimHoldInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        body.set(new ClaimHoldInput(request.user().trim()));
    }
}
