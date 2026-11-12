package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses holding or releasing a claim that is not OPEN or PENDING. */
public class CheckHoldClaim {
    public void check(@Val Claim claim) throws InvalidSheetException {
        if (!claim.getStatus().isOpen()) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim can be held or released");
        }
    }
}
