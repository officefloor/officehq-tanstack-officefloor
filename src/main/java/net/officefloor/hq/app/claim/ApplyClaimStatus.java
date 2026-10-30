package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Out<ClaimNumber> number)
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
        if (request.target() == ClaimStatus.SETTLED) {
            // Settling fixes the due at the rate in force on the claim's date; with no rate it stays live (n/a until one is set).
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            claim.setSettledDue(Rates.due(claim.getAmount(), rates.find(claim.getSheetId(), grade, claim.getDate())));
        }
        claim.setStatus(request.target());
    }
}
