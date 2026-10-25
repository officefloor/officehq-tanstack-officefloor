package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordClaimReassigned {
    public void record(@Val Claim claim, @Val ReassignTo to, @Val ChangedBy by, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REASSIGNED", claim, by, to.toName()) + " from=" + to.fromName() + " to="
                + to.toName());
    }
}
