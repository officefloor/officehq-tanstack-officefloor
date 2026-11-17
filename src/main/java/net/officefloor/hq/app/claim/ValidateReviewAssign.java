package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the user a REVIEW claim is assigned to, and who assigns it. */
public class ValidateReviewAssign {
    public void validate(@RequestBody ReviewAssignRequest request, Out<ClaimReviewAssignment> assignment) {
        assignment.set(new ClaimReviewAssignment(request == null || request.user() == null ? "" : request.user().trim(),
                request == null || request.by() == null ? "" : request.by().trim()));
    }
}
