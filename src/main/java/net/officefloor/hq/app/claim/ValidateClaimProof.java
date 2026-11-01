package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimProof {
    public void validate(@RequestBody ClaimProofRequest request, Out<ClaimProof> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String user = request == null ? null : request.user();
        String kind = request == null || request.kind() == null ? null : request.kind().trim();
        if (kind != null && kind.isEmpty()) kind = null;
        if (kind != null && !ProofKind.ALL.contains(kind)) {
            throw new InvalidSheetRequestException("A proof's kind must be receipt or photo");
        }
        actor.set(new Actor(user));
        body.set(new ClaimProof(user, kind));
    }
}
