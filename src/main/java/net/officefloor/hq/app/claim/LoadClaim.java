package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            ClaimRepository repository, Out<Claim> claimOut) throws ClaimNotFoundException {
        Claim claim = repository.findById(sheetId, claimId);
        if (claim == null) throw new ClaimNotFoundException(claimId);
        claimOut.set(claim);
    }
}
