package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;

public class AuditClaimStatus {
    public void audit(@Val ValidClaimStatus request, @Val Claim claim, People people, Audit audit) {
        String event = switch (request.target()) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> throw new IllegalStateException("A claim never returns to DRAFT");
        };
        audit.recordOnCommit(ClaimAudit.line(event, request.user(),
                people.find(claim.getSheetId(), claim.getPersonId()), claim));
    }
}
