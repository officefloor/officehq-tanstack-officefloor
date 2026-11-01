package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimHold {
    public void validate(@RequestBody ClaimHoldRequest request, Out<ClaimHold> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who holds the claim must be named");
        actor.set(new Actor(user));
        body.set(new ClaimHold(user));
    }
}
