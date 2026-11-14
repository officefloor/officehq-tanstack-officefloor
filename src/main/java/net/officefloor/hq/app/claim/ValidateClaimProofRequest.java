package net.officefloor.hq.app.claim;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a proof of a kind other than receipt or photo; publishes the kind chosen (none: null). */
public class ValidateClaimProofRequest {
    public void validate(@RequestBody @Valid ClaimProofRequest request, Out<ProofKind> kind) throws InvalidClaimException {
        String value = request == null || request.kind() == null ? "" : request.kind().trim();
        if (value.isEmpty()) {
            kind.set(new ProofKind(null));
            return;
        }
        if (!ProofKind.isKind(value)) {
            throw new InvalidClaimException("A proof is a receipt or a photo.");
        }
        kind.set(new ProofKind(value));
    }
}
