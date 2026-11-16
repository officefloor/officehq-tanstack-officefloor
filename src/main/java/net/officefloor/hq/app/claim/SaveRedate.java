package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's new date and number. A change another user made first (the claim re-dated or its status
 *  changed) is refused. */
public class SaveRedate {
    public void save(@Val Claim claim, @Val ClaimRedating redating, ClaimRepository claims, Out<Claim> saved)
            throws InvalidClaimStatusException {
        if (!claims.redate(claim, redating.old())) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claims.find(claim.sheetId(), claim.id()));
    }
}
