package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, and a kind (when chosen) that is a proof
 *  kind (receipt or photo). */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ProofInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String kind = request.kind() == null || request.kind().isBlank() ? null : request.kind().trim();
        if (kind != null && !Claim.PROOF_KINDS.contains(kind)) {
            throw new InvalidClaimException("A proof is a " + String.join(" or ", Claim.PROOF_KINDS) + ".");
        }
        body.set(new ProofInput(request.user().trim(), kind));
    }
}
