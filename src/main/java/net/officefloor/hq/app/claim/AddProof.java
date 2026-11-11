package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Adds a proof of the kind chosen (none: a receipt) to the claim: not to a SETTLED or VOID claim. */
public class AddProof {
    public void apply(@Val Claim claim, @Val ProofKind kind) throws InvalidClaimStatusException {
        claim.addProof(kind.kind());
    }
}
