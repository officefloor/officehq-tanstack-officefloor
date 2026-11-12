package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored move: CLAIM_REASSIGNED sheet=<id> by=<user> person=<new name> amount=<0.00>
 *  date=<yyyy-mm-dd>, then number=<n> when the claim has one, then from=<old name> to=<new name>. */
public class AuditClaimReassigned {
    public void audit(@Val ReassignInput input, @Val Claim claim, @Val Reassignment reassignment, @Val Rollup before,
            Audit audit, RollupService rollups) {
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_REASSIGNED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + reassignment.to().getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber())
                + " from=" + reassignment.from().getName() + " to=" + reassignment.to().getName());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
