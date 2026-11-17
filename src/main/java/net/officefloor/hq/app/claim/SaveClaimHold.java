package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the hold or release, but only while the claim is still OPEN or PENDING and not already so; a change another
 *  user made first is refused. Nothing is written when the claim already was as asked. */
public class SaveClaimHold {
    public void save(@Val Claim claim, @Val ClaimHoldChange change, ClaimRepository claims)
            throws InvalidClaimStatusException {
        if (change.changed() && !claims.hold(claim)) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
    }
}
