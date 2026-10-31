package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimProof {
    public void validate(@RequestBody ClaimProofRequest request, Out<ClaimProof> body, Out<Actor> actor) {
        String user = request == null ? null : request.user();
        actor.set(new Actor(user));
        body.set(new ClaimProof(user));
    }
}
