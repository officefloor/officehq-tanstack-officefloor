package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordClaimAdded {
    public void record(@Val Claim claim, @Val Person person, @Val ChangedBy by, Audit audit, ClaimLog log) {
        audit.record(ClaimAudit.added(claim, by, person.getName()));
        if (claim.isLateEntry()) {
            audit.record(ClaimAudit.line("LATE_ENTRY", claim, by, person.getName()));
        }
        log.record(claim, by);
    }
}
