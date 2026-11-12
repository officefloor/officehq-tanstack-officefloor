package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Locks the claim for the user opening its editor; refused while another user holds its lock. */
public class ApplyLock {
    public void apply(@Val LockInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (!store.lock(claim, input.user())) {
            throw new InvalidSheetException("The claim is being edited by another user");
        }
    }
}
