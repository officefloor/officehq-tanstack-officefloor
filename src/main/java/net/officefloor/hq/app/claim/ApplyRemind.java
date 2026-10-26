package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyRemind {
    // Only an OPEN or PENDING claim is reminded, once per sheet's today. Stored only if the claim is still OPEN or
    // PENDING and not reminded today, so a claim settled or reminded meanwhile by another user is refused.
    public void apply(@Val Claim claim, Claims claims, Sheets sheets, Out<ClaimResponse> responseOut)
            throws ClaimRemindRefusedException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new ClaimRemindRefusedException("Only an OPEN or PENDING claim can be reminded.");
        }
        LocalDate today = sheets.today(claim.sheetId());
        if (today.equals(claim.remindedOn())) {
            throw new ClaimRemindRefusedException("The claim was already reminded today (" + today + ").");
        }
        Claim reminded = claims.remind(claim, today);
        if (reminded == null) {
            throw new ClaimRemindRefusedException("The claim changed meanwhile: try again.");
        }
        responseOut.set(ClaimResponse.from(reminded));
    }
}
