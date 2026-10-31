package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Edits the claim, refusing when it is no longer a DRAFT, or is dated in a closed period, or would be moved into one. */
public class ApplyClaim {
    public void apply(@Val ClaimValues values, @Val Claim claim, Sheets sheets, ClosedPeriods closedPeriods)
            throws ClaimEditRefusedException, PeriodClosedException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimEditRefusedException(claim.getStatus());
        }
        LocalDate date = values.date() != null ? values.date() : sheets.today(claim.getSheetId());
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        closedPeriods.refuse(claim.getSheetId(), date);
        claim.setAmount(values.amount());
        claim.setDate(date);
        claim.setNote(values.note());
    }
}
