package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            ClaimRepository repository, Out<Claim> claim) throws ClaimNotFoundException {
        Claim found = repository.find(sheetId, claimId);
        if (found == null) {
            throw new ClaimNotFoundException(claimId);
        }
        claim.set(found);
    }
}
