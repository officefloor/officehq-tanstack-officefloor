package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimRemind {
    public void validate(@RequestBody ClaimRemindRequest request, Out<ClaimRemind> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidSheetRequestException("Who reminds the claim must be named");
        actor.set(new Actor(user));
        body.set(new ClaimRemind(user));
    }
}
