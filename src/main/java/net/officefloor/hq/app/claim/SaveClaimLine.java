package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimLine {
    public void save(@Val Claim claim, @Val ClaimLine line, ClaimLineRepository lines, ClaimRepository claims) {
        lines.add(claim, line);
        claims.update(claim);
    }
}
