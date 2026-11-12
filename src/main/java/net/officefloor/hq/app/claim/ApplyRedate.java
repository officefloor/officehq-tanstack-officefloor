package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's new date, keeping its due-by and any figure a statement bound it to. Its period
 *  is the one its due-by falls in, so it stays as it is, and so does its number. */
public class ApplyRedate {
    public void apply(@Val RedateInput input, @Val Claim claim, ClaimStore store, Out<Redating> redating)
            throws InvalidSheetException {
        LocalDate old = claim.getDate();
        if (!store.redate(claim, input.date(), claim.getNumber())) {
            throw new InvalidSheetException("The claim has moved on");
        }
        redating.set(new Redating(old, input.date()));
    }
}
