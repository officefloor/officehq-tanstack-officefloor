package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Val;

public class SaveClaimShare {
    public void save(@Val Claim claim, @Val ClaimShare share, ClaimRepository claims) {
        claims.addShare(claim, share);
        List<ClaimShare> shares = new ArrayList<>(claim.getShares());
        shares.add(share);
        claim.setShares(shares);
    }
}
