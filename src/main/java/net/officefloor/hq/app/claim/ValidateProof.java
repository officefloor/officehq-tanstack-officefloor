package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the proof's kind: receipt or photo, or none chosen. Any other kind is refused. */
public class ValidateProof {
    public void validate(@RequestBody ProofRequest request, Out<ProofKind> body)
            throws InvalidClaimException {
        String kind = request == null || request.kind() == null ? "" : request.kind().trim();
        if (kind.isEmpty()) {
            body.set(new ProofKind(null));
            return;
        }
        if (!ProofKind.KINDS.contains(kind)) throw new InvalidClaimException("A proof's kind is receipt or photo");
        body.set(new ProofKind(kind));
    }
}
