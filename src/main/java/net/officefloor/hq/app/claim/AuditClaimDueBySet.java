package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimDueBySet {
    public void audit(@Val ValidDueBy request, @Val Claim claim, @Val ClaimDueBy dueBy, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_DUE_BY_SET", request.user(), person, claim, claim.getNumber())
                + " old=" + (dueBy.old() == null ? "" : dueBy.old()) + " new=" + claim.getDueBy());
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
