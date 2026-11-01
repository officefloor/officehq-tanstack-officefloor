package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimReviewAssign {
    public void validate(@RequestBody ClaimReviewAssignRequest request, Out<ClaimReviewAssign> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who assigns the review must be named");
        String assignee = request.assignee() == null ? "" : request.assignee().trim();
        if (assignee.isEmpty()) throw new InvalidSheetRequestException("The user the review is assigned to must be named");
        actor.set(new Actor(user));
        body.set(new ClaimReviewAssign(user, assignee));
    }
}
