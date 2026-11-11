package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") String claimId,
            ClaimStore store, Out<Claim> claimOut) throws InvalidSheetException {
        Claim claim = null;
        try {
            claim = store.find(sheetId.trim(), Long.parseLong(claimId.trim()));
        } catch (NumberFormatException e) {
            // not an id: no such claim
        }
        if (claim == null) {
            throw new InvalidSheetException("No such claim: " + claimId);
        }
        claimOut.set(claim);
    }
}
