package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Keeps the line on the claim and the claim's new amount. */
public class SaveLine {
    public void save(@Val ClaimLine line, @Val Claim claim, Claims claims) {
        claims.addLine(claim, line);
        claims.save(claim);
    }
}
