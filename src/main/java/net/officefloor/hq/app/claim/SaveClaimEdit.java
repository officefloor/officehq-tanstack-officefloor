package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores an edit of a draft, refusing it when another user moved the claim on first. */
public class SaveClaimEdit {
    public void save(@Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (!store.edit(claim)) {
            throw new InvalidSheetException("Only a DRAFT claim can be edited");
        }
    }
}
