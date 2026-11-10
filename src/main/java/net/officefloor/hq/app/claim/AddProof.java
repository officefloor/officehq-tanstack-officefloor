package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Adds a proof to the claim: not to a SETTLED or VOID claim. */
public class AddProof {
    public void apply(@Val Claim claim) throws InvalidClaimStatusException {
        claim.addProof();
    }
}
