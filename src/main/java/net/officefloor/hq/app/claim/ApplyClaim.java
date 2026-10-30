package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, Sheets sheets, Periods periods)
            throws InvalidClaimException {
        LocalDate date = request.date() != null ? request.date() : sheets.today(claim.getSheetId());
        if (periods.isClosed(claim.getSheetId(), claim.getDate()) || periods.isClosed(claim.getSheetId(), date)) {
            throw new InvalidClaimException("No claim dated inside a closed period can be edited");
        }
        claim.setAmount(request.amount());
        claim.setDate(date);
        claim.setNote(request.note());
    }
}
