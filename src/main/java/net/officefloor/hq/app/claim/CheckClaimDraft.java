package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses an edit of a claim that is no longer a draft: an open claim is voided and added anew instead. */
public class CheckClaimDraft {
    public void check(@Val Claim claim) throws InvalidSheetException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidSheetException("A " + claim.getStatus() + " claim cannot be edited");
        }
    }
}
