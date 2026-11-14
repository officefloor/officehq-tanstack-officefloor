package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the sheet's today as the day the claim was last reminded; escalates the claim (once ever) when the sheet has
 * an escalation gap and the previous reminder was at least that many days before today.
 */
public class RemindClaim {
    public void remind(@Val Claim claim, ClaimRepository repository, SheetRepository sheets, SettingRepository settings,
            Out<ClaimEscalation> escalation) {
        LocalDate today = sheets.today(claim.sheetId());
        LocalDate previous = repository.reminded(claim.sheetId(), claim.id());
        Integer gap = settings.escalateAfter(claim.sheetId());
        boolean escalate = gap != null && previous != null && !previous.plusDays(gap).isAfter(today)
                && !repository.escalated(claim.sheetId(), claim.id());
        repository.remind(claim.sheetId(), claim.id(), today);
        if (escalate) {
            repository.escalate(claim.sheetId(), claim.id());
        }
        escalation.set(new ClaimEscalation(escalate));
    }
}
