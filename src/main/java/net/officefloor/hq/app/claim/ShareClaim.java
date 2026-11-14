package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Gives the person their share of the claim. */
public class ShareClaim {
    public void share(@Val Claim claim, @Val ClaimShare share, ClaimShareRepository shares) {
        shares.append(claim.sheetId(), share);
    }
}
