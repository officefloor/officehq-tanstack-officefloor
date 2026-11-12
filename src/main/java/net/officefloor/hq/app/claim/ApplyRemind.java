package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the claim reminded on the sheet's today; refused when another user moved it on or reminded it first.
 *  A reminder at least the sheet's escalate-after days after the claim's previous one escalates it, once only. */
public class ApplyRemind {
    public void apply(@Val Claim claim, ClaimStore store, SheetStore sheets, SettingStore settings,
            Out<Escalation> escalation) throws InvalidSheetException {
        LocalDate previous = claim.getRemindedOn();
        LocalDate today = sheets.today(claim.getSheetId());
        if (!store.remind(claim, today)) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim not yet reminded today can be reminded");
        }
        Integer gap = settings.escalateAfter(claim.getSheetId());
        boolean due = gap != null && previous != null && !previous.plusDays(gap).isAfter(today) && !claim.isEscalated();
        escalation.set(new Escalation(due && store.escalate(claim)));
    }
}
