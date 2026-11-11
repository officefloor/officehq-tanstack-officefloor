package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reminds an OPEN or PENDING claim on the sheet's today, once per day: any other status, or a
 * second reminder the same day, is refused and writes nothing. While the sheet's escalation gap is
 * set, a reminder whose previous one was at least that many days before the sheet's today escalates
 * the claim, once per claim ever.
 */
public class RemindClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository, SettingsRepository settings,
            Out<ClaimEscalation> escalation) throws InvalidClaimStatusException {
        LocalDate previous = claim.getRemindedOn();
        claim.remind(sheet.today());
        if (!repository.remind(claim)) throw new InvalidClaimStatusException("The claim was already reminded today");
        Integer gap = settings.find(sheet.id()).escalateAfter();
        boolean due = gap != null && previous != null && !previous.plusDays(gap).isAfter(sheet.today());
        escalation.set(new ClaimEscalation(due && repository.escalate(claim)));
    }
}
