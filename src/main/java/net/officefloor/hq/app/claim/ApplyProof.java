package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one, a proof of the kind chosen (a receipt when none is); refused for a SETTLED or VOID claim, or when another user moved it on first. */
public class ApplyProof {
    public void apply(@Val ProofInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID || !store.addProof(claim, input.kind())) {
            throw new InvalidSheetException("A proof cannot be added to a " + claim.getStatus() + " claim");
        }
    }
}
