package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores a reminder on the sheet's today, but only while the claim is OPEN or PENDING and not yet reminded that day.
 *  While the sheet's escalation gap is set, a claim whose previous reminder was at least that many days before the
 *  sheet's today is escalated, once per claim ever. */
public class SaveReminder {
    public void save(@Val Claim claim, ClaimRepository claims, SheetRepository sheets, SettingRepository settings,
            Out<ClaimEscalation> escalation) throws InvalidClaimStatusException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("Only an OPEN or PENDING claim can be reminded");
        }
        LocalDate today = sheets.today(claim.sheetId());
        LocalDate previous = claims.remindedOn(claim.sheetId(), claim.id());
        if (!claims.remind(claim, today)) {
            throw new InvalidClaimStatusException("The claim was already reminded today");
        }
        Integer gap = settings.escalateAfter(claim.sheetId());
        boolean escalated = gap != null && previous != null && ChronoUnit.DAYS.between(previous, today) >= gap
                && claims.escalate(claim);
        escalation.set(new ClaimEscalation(escalated));
    }
}
