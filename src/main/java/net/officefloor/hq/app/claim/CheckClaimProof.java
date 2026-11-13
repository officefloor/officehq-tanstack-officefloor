package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses (400) a proof for a SETTLED or VOID claim. */
public class CheckClaimProof {
    public void check(@Val Claim claim) throws InvalidClaimException {
        if (!claim.takesProof()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be given a proof.");
        }
    }
}
