package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Keeps the line on the draft, added on the sheet's today, and makes the claim's amount the sum of its lines;
 *  an edit of the claim, so the user becomes its edited-by. Refused when another user moved the claim on first. */
public class ApplyLine {
    public void apply(@Val LineInput input, @Val Claim claim, ClaimLineStore lines, ClaimStore store, SheetStore sheets)
            throws InvalidSheetException {
        lines.add(claim, input, sheets.today(claim.getSheetId()));
        claim.setAmount(lines.total(claim));
        claim.setEditedBy(input.user());
        if (!store.editAmount(claim)) {
            throw new InvalidSheetException("Only a DRAFT claim can be edited");
        }
    }
}
