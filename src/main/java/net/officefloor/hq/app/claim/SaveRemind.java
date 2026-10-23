package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Remind the loaded claim on the sheet's today: only an OPEN or PENDING claim, and only once per sheet-today. A
 *  reminder another user's change got to first is refused. While the sheet has an escalation gap, a reminder whose
 *  claim's previous one was at least that many days before the sheet's today escalates the claim, once ever. */
public class SaveRemind {
    public void save(@Val Claim loaded, SheetRepository sheets, SettingRepository settings,
            ClaimRepository repository, Out<ClaimResponse> saved, Out<Escalation> escalation)
            throws ClaimStatusNotAllowedException {
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be reminded; only an OPEN or PENDING one can.");
        }
        var today = sheets.today(loaded.sheetId());
        if (today == null) {
            throw new ClaimStatusNotAllowedException("Set the sheet's today before reminding a claim.");
        }
        if (today.equals(loaded.reminded())) {
            throw new ClaimStatusNotAllowedException("The claim was already reminded on " + today + ".");
        }
        if (!repository.remind(loaded, today)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        Integer gap = settings.escalateAfter(loaded.sheetId());
        boolean due = gap != null && loaded.reminded() != null && !loaded.reminded().plusDays(gap).isAfter(today);
        escalation.set(new Escalation(due && repository.escalate(loaded)));
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(),
                loaded.deferredUntil(), loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), today, loaded.ref(), loaded.tags(), loaded.waived()), List.of(),
                today));
    }
}
