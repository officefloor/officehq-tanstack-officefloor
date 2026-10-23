package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and a user to review the claim. */
public class ValidateReviewAssign {
    public void validate(@RequestBody ReviewAssignRequest request, Out<ReviewAssignInput> body)
            throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        if (request.reviewer() == null || request.reviewer().isBlank()) {
            throw new InvalidClaimException("Give the user to review the claim.");
        }
        body.set(new ReviewAssignInput(request.user().trim(), request.reviewer().trim()));
    }
}
