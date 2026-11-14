package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Raises a claim's proofs count by one. Refused for a claim dated in a closed period, and for a
 *  SETTLED or VOID claim. */
public class AddProof {
    public void add(@Val Claim claim, PeriodRepository periods)
            throws ClaimStatusException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        if (ClaimStatus.SETTLED.name().equals(claim.getStatus()) || ClaimStatus.VOID.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot gain a proof");
        }
        claim.setProofs(claim.getProofs() + 1);
    }
}
