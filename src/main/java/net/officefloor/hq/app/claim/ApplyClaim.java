package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Edits a claim: only a DRAFT can be edited (to change an open claim, void it and add a new one). */
public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, SheetRepository sheets,
            PeriodRepository periods) throws ClaimStatusException, PeriodClosedException {
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be edited");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        claim.setAmount(request.amount().toPlainString());
        claim.setDate((request.date() != null ? request.date() : sheets.today(claim.getSheetId())).toString());
        claim.setNote(request.note());
        periods.requireOpen(claim.getSheetId(), claim.getDate());
    }
}
