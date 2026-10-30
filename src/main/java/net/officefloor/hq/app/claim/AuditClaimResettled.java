package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimResettled {
    public void audit(@Val ResettleRequest request, @Val Claim claim, @Val ClaimResettle resettle, People people,
            Audit audit, @Val RollupFigures before, Rollup rollup, ClaimLog log) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        log.record(claim, person, request.user());
        audit.recordOnCommit(ClaimAudit.line("CLAIM_RESETTLED", request.user(), person, claim, claim.getNumber())
                + " old=" + show(resettle.oldDue()) + " new=" + show(resettle.newDue()));
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }

    private static String show(BigDecimal due) {
        return due == null ? "n/a" : due.setScale(2).toPlainString();
    }
}
