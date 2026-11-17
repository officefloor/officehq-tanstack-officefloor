package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's due-by as set. A change another user made first (its due-by or status changed) is refused. */
public class SaveDueBy {
    public void save(@Val Claim claim, @Val ClaimDueBySetting setting, ClaimRepository claims, Out<Claim> saved)
            throws InvalidClaimStatusException {
        if (!claims.setDueBy(claim, setting.old())) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claims.find(claim.sheetId(), claim.id()));
    }
}
