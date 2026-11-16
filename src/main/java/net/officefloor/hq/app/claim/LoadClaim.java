package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a claim of the sheet by id; one not on the sheet is a 404. */
public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            ClaimRepository claims, Out<Claim> claim) throws ClaimNotFoundException {
        Claim found;
        try {
            found = claims.find(sheetId.trim(), Long.parseLong(claimId.trim()));
        } catch (NumberFormatException e) {
            found = null;
        }
        if (found == null) {
            throw new ClaimNotFoundException("No claim " + claimId);
        }
        claim.set(found);
    }
}
