package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("claimId") String claimId, @Val Sheet sheet,
            ClaimRepository repository, Out<Claim> claimOut) throws ClaimNotFoundException {
        Claim claim = null;
        try {
            claim = repository.find(sheet.id(), Long.parseLong(claimId));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (claim == null) throw new ClaimNotFoundException(claimId);
        claimOut.set(claim);
    }
}
