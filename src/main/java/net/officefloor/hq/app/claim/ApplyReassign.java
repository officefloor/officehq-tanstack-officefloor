package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to its new person: it keeps its number and date, loses any figure a statement
 *  bound it to, and is listed as their newest claim. */
public class ApplyReassign {
    public void apply(@Val Claim claim, @Val Reassignment reassignment, ClaimStore store) throws InvalidSheetException {
        if (!store.reassign(claim, reassignment.to().getId())) {
            throw new InvalidSheetException("The claim has moved on");
        }
    }
}
