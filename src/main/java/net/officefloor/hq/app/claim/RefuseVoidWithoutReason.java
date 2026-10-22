package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Val;

/** While the sheet's list of void reasons is not empty, refuse a void that does not give a reason on it. */
public class RefuseVoidWithoutReason {
    public void refuse(@Val ClaimStatusInput input, @Val Claim loaded, VoidReasonRepository reasons)
            throws ClaimStatusNotAllowedException {
        if (!input.action().equals("void") || !reasons.any(loaded.sheetId())) {
            return;
        }
        if (input.reason() == null) {
            throw new ClaimStatusNotAllowedException("Choose a void reason to void this claim.");
        }
        if (!reasons.exists(loaded.sheetId(), input.reason())) {
            throw new ClaimStatusNotAllowedException(input.reason() + " is not a void reason on this sheet.");
        }
    }
}
