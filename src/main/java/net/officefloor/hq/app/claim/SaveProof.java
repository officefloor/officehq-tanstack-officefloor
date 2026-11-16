package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one, but only while it is still DRAFT, OPEN or PENDING; a change another user
 *  made first is refused. Publishes the claim as stored. */
public class SaveProof {
    public void save(@Val Claim claim, ClaimRepository claims, Out<Claim> saved) throws InvalidClaimStatusException {
        if (!claims.addProof(claim)) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claims.find(claim.sheetId(), claim.id()));
    }
}
