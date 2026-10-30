package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimHeld {
    public void audit(@Val HoldClaimRequest request, @Val Claim claim, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line(claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED", request.user(), person, claim,
                claim.getNumber()));
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
