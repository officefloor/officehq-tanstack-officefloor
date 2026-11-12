package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a line on a claim another user has locked by opening its editor. */
public class CheckLineLock {
    public void check(@Val LineInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (store.lockedByOther(claim, input.user())) {
            throw new InvalidSheetException("The claim is being edited by another user");
        }
    }
}
