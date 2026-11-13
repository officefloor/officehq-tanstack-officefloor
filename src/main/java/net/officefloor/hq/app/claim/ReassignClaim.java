package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new person, keeping its date; it loses any statement figure. A numbered claim takes the next
 * number in the sequence opening would number it in now (the sheet's, or its period's under period numbering); its
 * old number is never used again. A draft, having no number, gets none.
 */
public class ReassignClaim {
    public void reassign(@Val Claim claim, @Val Reassignment reassignment, ClaimRepository repository,
            SheetRepository sheets, SettingRepository settings, PeriodRepository periods, Out<Claim> moved) {
        Claim reassigned = repository.reassign(claim, reassignment.to().id());
        if (reassigned.number() != null) {
            String next = ApplyClaimAction.nextNumber(reassigned, sheets, settings, periods);
            reassigned = reassigned.withStatus(reassigned.status(), next, reassigned.due(), reassigned.openedBy());
            repository.renumber(reassigned);
            reassigned = repository.find(reassigned.sheetId(), reassigned.id());
        }
        moved.set(reassigned);
    }
}
