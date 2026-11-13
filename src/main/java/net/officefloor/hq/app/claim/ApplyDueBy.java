package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's new due-by, set directly: a holiday added later leaves it as set. Overdue, the late charge,
 *  grace and the claim's period follow it. */
public class ApplyDueBy {
    public void apply(@Val DueByInput input, @Val Claim claim, ClaimStore store, Out<DueBySetting> setting)
            throws InvalidSheetException {
        LocalDate old = claim.getDueBy();
        if (!store.setDueBy(claim, input.dueBy())) {
            throw new InvalidSheetException("The claim has moved on");
        }
        setting.set(new DueBySetting(old, input.dueBy()));
    }
}
