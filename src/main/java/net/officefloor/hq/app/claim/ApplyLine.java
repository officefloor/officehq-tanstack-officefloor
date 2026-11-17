package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the line to the claim's amount: with lines, a claim's amount is the quantities times the unit amounts added
 *  up. Adding a line edits the claim, so only a DRAFT claim whose due-by is not inside a closed period, and that no
 *  other user is editing (holds its lock), takes one. */
public class ApplyLine {
    public void apply(@Val Claim claim, @Val ClaimLine line, ClaimRepository claims, PeriodRepository periods,
            Out<Claim> changed) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a DRAFT claim can have lines added");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimException("The claim's due-by is inside a closed period");
        }
        String lockedBy = claims.lockedBy(claim.sheetId(), claim.id());
        if (lockedBy != null && !lockedBy.equals(line.by())) {
            throw new InvalidClaimException("Another user is editing the claim");
        }
        BigDecimal amount = line.total();
        for (ClaimLine l : claims.lines(claim.sheetId(), claim.id())) {
            amount = amount.add(l.total());
        }
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), amount.setScale(2), claim.date(), claim.note(),
                claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(),
                claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags(), claim.held()));
    }
}
