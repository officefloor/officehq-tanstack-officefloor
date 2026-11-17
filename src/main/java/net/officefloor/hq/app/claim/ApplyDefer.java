package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Defers the claim until the given date (see ClaimDue#deferred). Only an OPEN claim is deferred, only to a date after
 * the sheet's today; a claim whose due-by falls inside a closed period is refused.
 */
public class ApplyDefer {
    public void apply(@Val Claim claim, @Val ClaimDeferral deferral, SheetRepository sheets, PeriodRepository periods,
            Out<Claim> changed) throws InvalidClaimStatusException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusException("Only an OPEN claim can be deferred");
        }
        if (!deferral.until().isAfter(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("A claim is deferred only to a date after today");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(),
                claim.statedDue(), claim.corrects(), claim.dueBy(), deferral.until(), claim.proofs(), claim.splitFrom(), claim.ref(), claim.tags(), claim.held()));
    }
}
