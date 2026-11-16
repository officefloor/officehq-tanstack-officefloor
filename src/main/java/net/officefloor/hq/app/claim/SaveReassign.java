package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the move: the claim keeps its number and date, loses any figure a statement bound it to (its due is live
 * again until its new person's next statement) and is listed as its new person's newest claim. A change another user
 * made first (the claim moved or its status changed) is refused.
 */
public class SaveReassign {
    public void save(@Val Claim claim, @Val ClaimReassignment reassignment, ClaimRepository claims, Out<Claim> saved)
            throws InvalidClaimStatusException {
        if (!claims.reassign(claim, reassignment.from().id(), reassignment.to().id())) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claims.find(claim.sheetId(), claim.id()));
    }
}
