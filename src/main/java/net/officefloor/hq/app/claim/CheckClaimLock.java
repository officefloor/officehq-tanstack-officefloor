package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses an edit of a claim another user has locked by opening its editor. */
public class CheckClaimLock {
    public void check(@Val ClaimInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (store.lockedByOther(claim, input.user() == null ? "" : input.user().trim())) {
            throw new InvalidSheetException("The claim is being edited by another user");
        }
    }
}
