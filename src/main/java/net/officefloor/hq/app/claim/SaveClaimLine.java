package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimLine {
    public void save(@Val ClaimLine line, @Val Claim claim, ClaimLines lines, Claims claims) {
        lines.write(claim, line);
        claims.save(claim);
    }
}
