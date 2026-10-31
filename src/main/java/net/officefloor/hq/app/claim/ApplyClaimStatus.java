package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; voiding clears it (the number is not reused).
 *  Settling fixes the claim's due at its person's grade's rate now; with no rate it stays live. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim, Sheets sheets, People people,
            Rates rates, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        claim.setStatus(change.action().to());
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
        }
        if (change.action() == ClaimAction.SETTLE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            BigDecimal pct = rates.pct(claim.getSheetId(), grade);
            claim.setDue(pct == null ? null
                    : claim.getAmount().multiply(pct).divide(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }
        number.set(new ClaimNumber(claim.getNumber()));
        if (change.action() == ClaimAction.VOID) {
            claim.setNumber(null);
        }
    }
}
