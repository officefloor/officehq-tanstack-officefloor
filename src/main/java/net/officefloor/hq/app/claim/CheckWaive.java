package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses waiving a claim that is not OPEN or PENDING, or one already waived. */
public class CheckWaive {
    public void check(@Val Claim claim) throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim can have its late charge waived");
        }
        if (claim.isWaived()) {
            throw new InvalidSheetException("The claim's late charge is already waived");
        }
    }
}
