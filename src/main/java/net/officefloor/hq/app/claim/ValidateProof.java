package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the proof body once, refusing a kind that is neither receipt nor photo; none chosen publishes "". */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ProofRequest> body) throws InvalidClaimStatusChangeException {
        String kind = request == null || request.kind() == null ? "" : request.kind().trim();
        if (!kind.isEmpty() && !ProofKind.ALL.contains(kind)) {
            throw new InvalidClaimStatusChangeException("A proof is a receipt or a photo");
        }
        body.set(new ProofRequest(kind));
    }
}
