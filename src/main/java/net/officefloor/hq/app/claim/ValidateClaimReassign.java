package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimReassign {
    public void validate(@RequestBody ClaimReassignRequest request, Out<ClaimReassignment> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        if (request == null || request.toPersonId() == null) {
            throw new InvalidSheetRequestException("A reassignment needs the person to move the claim to");
        }
        actor.set(new Actor(request.user()));
        body.set(new ClaimReassignment(request.toPersonId(), request.user()));
    }
}
