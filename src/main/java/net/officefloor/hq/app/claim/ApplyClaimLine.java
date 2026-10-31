package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.plugin.variable.Val;

/** Adds the line to the claim, as an edit of it: refused when another user's editor locks it, it is no
 *  longer a DRAFT, or it is due by in a closed period. The claim's amount becomes its lines' quantities
 *  times their unit amounts added up, and the adding user becomes its last editor. */
public class ApplyClaimLine {
    public void apply(@Val ClaimLine line, @Val Claim claim, ClosedPeriods closedPeriods, ClaimLines lines)
            throws ClaimLockedException, ClaimEditRefusedException, PeriodClosedException {
        ApplyClaimLock.refuseLocked(claim, line.user());
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimEditRefusedException(claim.getStatus());
        }
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        BigDecimal total = lines.total(claim).add(line.unit().multiply(BigDecimal.valueOf(line.qty())));
        claim.setAmount(total.setScale(2));
        claim.setEditedBy(line.user());
    }
}
