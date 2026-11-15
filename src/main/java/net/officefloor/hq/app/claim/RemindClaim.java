package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Reminds an OPEN or PENDING claim on the sheet's today. Refused for a claim in any other status, and
 *  for one already reminded on the sheet's today. */
public class RemindClaim {
    public void remind(@Val Claim claim, SheetRepository sheets) throws ClaimStatusException {
        if (!ClaimStatus.isOpen(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be reminded");
        }
        String today = sheets.today(claim.getSheetId()).toString();
        if (today.equals(claim.getReminded())) {
            throw new ClaimStatusException("The claim was already reminded on " + today);
        }
        claim.setReminded(today);
    }
}
