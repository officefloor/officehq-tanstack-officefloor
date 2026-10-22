package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Fails when the claim is not on the sheet. */
public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            ClaimRepository repository, Out<Claim> claim) throws ClaimNotFoundException {
        Claim found = null;
        try {
            found = repository.find(sheetId.trim(), Long.parseLong(claimId.trim()));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (found == null) {
            throw new ClaimNotFoundException(claimId);
        }
        claim.set(found);
    }
}
