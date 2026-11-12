package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to its new person: it keeps its date, takes the next number in the sequence it would be
 *  numbered in (its old one is never used again; a draft, having none, gets none), loses any figure a
 *  statement bound it to, and is listed as their newest claim. */
public class ApplyReassign {
    public void apply(@Val Claim claim, @Val Reassignment reassignment, ClaimStore store, SettingStore settings,
            SheetStore sheets, PeriodStore periods) throws InvalidSheetException {
        String number = claim.getNumber() == null ? null
                : Numbering.next(claim.getSheetId(), claim.getDueBy(), store, settings, sheets, periods);
        if (!store.reassign(claim, reassignment.to().getId(), number)) {
            throw new InvalidSheetException("The claim has moved on");
        }
    }
}
