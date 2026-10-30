package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimReassigned {
    public void audit(@Val ReassignRequest request, @Val Claim claim, @Val ClaimMove move, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        audit.recordOnCommit(ClaimAudit.line("CLAIM_REASSIGNED", request.user(), move.to(), claim, claim.getNumber())
                + " from=" + move.from().getName() + " to=" + move.to().getName());
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
