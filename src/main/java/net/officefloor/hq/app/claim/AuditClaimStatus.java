package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;

public class AuditClaimStatus {
    public void audit(@Val ValidClaimStatus request, @Val Claim claim, @Val ClaimNumber number, People people, Audit audit, @Val RollupFigures before, Rollup rollup) {
        String event = switch (request.target()) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> throw new IllegalStateException("A claim never returns to DRAFT");
        };
        String line = ClaimAudit.line(event, request.user(),
                people.find(claim.getSheetId(), claim.getPersonId()), claim, number.value());
        if (request.target() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        audit.recordOnCommit(line);
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
