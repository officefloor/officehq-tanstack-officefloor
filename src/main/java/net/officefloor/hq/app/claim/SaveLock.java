package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Locks the claim for the user opening its editor, but only while it is still a DRAFT and no one else holds it;
 *  refused otherwise (nothing is written). Publishes the claim as stored. */
public class SaveLock {
    public void save(@Val Claim claim, @Val ClaimLock lock, ClaimRepository claims, Out<Claim> saved)
            throws InvalidClaimStatusException {
        if (!claims.lock(claim, lock.by())) {
            throw new InvalidClaimStatusException("The claim is being edited by someone else");
        }
        saved.set(claim);
    }
}
