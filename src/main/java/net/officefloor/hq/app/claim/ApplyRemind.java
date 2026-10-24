package net.officefloor.hq.app.claim;

import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reminds an OPEN or PENDING claim: its reminded date becomes the sheet's today. A claim in any other status, or one
 * already reminded on the sheet's today, is refused. With the sheet's escalation gap set, a claim never escalated
 * whose previous reminder was at least that many days before the sheet's today is escalated.
 */
public class ApplyRemind {
    public void apply(@Val Claim claim, @Val Sheet sheet, ClaimRepository repository, Out<Escalation> escalation)
            throws ClaimStatusException {
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be reminded.");
        }
        if (sheet.getToday().equals(claim.getRemindedOn())) {
            throw new ClaimStatusException("This claim was already reminded on " + sheet.getToday() + ".");
        }
        Integer gap = sheet.getEscalateAfter();
        boolean escalate = gap != null && !claim.isEscalated() && claim.getRemindedOn() != null
                && ChronoUnit.DAYS.between(claim.getRemindedOn(), sheet.getToday()) >= gap;
        if (escalate) {
            claim.setEscalated(true);
        }
        claim.setRemindedOn(sheet.getToday());
        repository.saveAndFlush(claim);
        escalation.set(new Escalation(escalate));
    }
}
