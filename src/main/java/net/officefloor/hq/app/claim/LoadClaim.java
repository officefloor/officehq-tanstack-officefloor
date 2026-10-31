package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            Claims claims, Out<Claim> claimOut) throws ClaimNotFoundException {
        Claim claim = claims.find(sheetId, claimId);
        if (claim == null) throw new ClaimNotFoundException(claimId);
        claimOut.set(claim);
    }
}
