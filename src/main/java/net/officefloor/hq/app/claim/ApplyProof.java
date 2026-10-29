package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyProof {
    // A SETTLED, VOID or DELETED claim takes no more proofs. Stored only if the claim still has its loaded status and count, so
    // a claim settled or voided meanwhile by another user is refused. A proof is a receipt or a photo; a receipt when none is chosen.
    public void apply(@RequestBody(required = false) ProofRequest body, @Val Claim claim, Claims claims,
            Out<ClaimResponse> responseOut, Out<ProofRequest> proofOut) throws ClaimProofRefusedException {
        ProofRequest proof = new ProofRequest(body == null ? "" : body.chosen());
        if (!proof.chosen().isEmpty() && !ProofRequest.KINDS.contains(proof.chosen())) {
            throw new ClaimProofRefusedException("A proof is a receipt or a photo.");
        }
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID
                || claim.status() == ClaimStatus.DELETED) {
            throw new ClaimProofRefusedException("A " + claim.status() + " claim takes no more proofs.");
        }
        Claim proved = claims.addProof(claim, proof.chosen().isEmpty() ? "receipt" : proof.chosen());
        if (proved == null) {
            throw new ClaimProofRefusedException("The claim changed meanwhile: try again.");
        }
        proofOut.set(proof);
        responseOut.set(ClaimResponse.from(proved));
    }
}
