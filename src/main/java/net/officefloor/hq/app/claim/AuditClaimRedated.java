package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimRedated {
    public void audit(@Val ValidRedate request, @Val Claim claim, @Val ClaimRedate redate, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_REDATED", request.user(), person, claim, claim.getNumber())
                + " old=" + redate.old() + " new=" + claim.getDate());
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
