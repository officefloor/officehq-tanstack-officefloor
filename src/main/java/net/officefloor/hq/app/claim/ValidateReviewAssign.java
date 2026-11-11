package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A review assignment needs a user, not blank. */
public class ValidateReviewAssign {
    public void validate(@RequestBody ReviewAssignRequest request, Out<ReviewAssignment> body)
            throws InvalidClaimException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidClaimException("A review is assigned to a user");
        body.set(new ReviewAssignment(user));
    }
}
