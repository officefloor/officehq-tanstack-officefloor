package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimComment {
    public void save(@Val ClaimComment comment, @Val Claim claim, ClaimComments comments) {
        comments.write(claim, comment);
    }
}
