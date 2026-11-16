package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Checks a proof may be added to the claim: not to a SETTLED or VOID claim, nor to one whose due-by falls inside a closed period.
 */
public class ApplyProof {
    public void apply(@Val Claim claim, PeriodRepository periods) throws InvalidClaimStatusException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim takes no more proofs");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
    }
}
