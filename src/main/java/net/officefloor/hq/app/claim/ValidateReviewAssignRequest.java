package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the review assignment's body once, publishing its user; refuses (400) a blank user. */
public class ValidateReviewAssignRequest {
    public void validate(@RequestBody ReviewAssignRequest request, Out<ReviewAssignee> assignee)
            throws InvalidClaimException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidClaimException("Type the user to assign the review to.");
        }
        assignee.set(new ReviewAssignee(user));
    }
}
