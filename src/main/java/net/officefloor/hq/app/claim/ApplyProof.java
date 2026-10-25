package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Raises the claim's proofs count by one, of the kind chosen (receipt or photo; when none is chosen, a receipt); a
 *  SETTLED or VOID claim, or an unknown kind, is refused. */
public class ApplyProof {
    public void apply(@Val Claim claim, ClaimRepository repository,
            @RequestParam(name = "kind", required = false) String kind) throws ClaimStatusException {
        String chosen = kind == null || kind.isBlank() ? null : kind.trim();
        if (chosen != null && !ProofKind.ALL.contains(chosen)) {
            throw new ClaimStatusException("A proof is a receipt or a photo.");
        }
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot gain a proof.");
        }
        claim.addProof(chosen != null ? chosen : ProofKind.RECEIPT);
        repository.saveAndFlush(claim);
    }
}
