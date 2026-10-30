package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        if (!claim.getStatus().canBecome(request.target())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
        }
        if (request.target() == ClaimStatus.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
        }
        number.set(new ClaimNumber(claim.getNumber()));
        if (request.target() == ClaimStatus.VOID) {
            claim.setNumber(null);
        }
        claim.setStatus(request.target());
    }
}
