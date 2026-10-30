package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditClaimLineAdded {
    public void audit(@Val LineRequest request, @Val ClaimLine line, @Val Claim claim, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        audit.recordOnCommit(ClaimAudit.line("CLAIM_LINE_ADDED", request.user(),
                people.find(claim.getSheetId(), claim.getPersonId()), claim, claim.getNumber())
                + " label=" + line.label() + " qty=" + line.qty() + " unit=" + line.unit().setScale(2).toPlainString());
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
