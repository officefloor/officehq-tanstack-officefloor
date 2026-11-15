package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim on the sheet's today. Refused for a claim in any other status, and
 *  for one already reminded on the sheet's today. With an escalation gap set, a claim never escalated whose
 *  previous reminder was at least that many days before the sheet's today is escalated by it. */
public class RemindClaim {
    public void remind(@Val Claim claim, SheetRepository sheets, SettingRepository settings, ClaimRepository claims)
            throws ClaimStatusException {
        if (!ClaimStatus.isOpen(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be reminded");
        }
        LocalDate today = sheets.today(claim.getSheetId());
        if (today.toString().equals(claim.getReminded())) {
            throw new ClaimStatusException("The claim was already reminded on " + today);
        }
        String gap = settings.find(claim.getSheetId(), SettingRepository.ESCALATE_AFTER);
        if (gap != null && !gap.isEmpty() && claim.getReminded() != null
                && ChronoUnit.DAYS.between(LocalDate.parse(claim.getReminded()), today) >= Integer.parseInt(gap)
                && !claims.isEscalated(claim.getSheetId(), claim.getId())) {
            claim.setEscalating(true);
        }
        claim.setReminded(today.toString());
    }
}
