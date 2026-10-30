package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;

public class AuditClaimStatus {
    public void audit(@Val ValidClaimStatus request, @Val Claim claim, @Val ClaimNumber number, People people, Audit audit, @Val RollupFigures before, Rollup rollup, ClaimLog log) {
        record(request, claim, number, people, audit, before, rollup, log);
    }

    /** Logs and audits a status change that went through, as a single change does. Bulk settle uses it too. */
    static void record(ValidClaimStatus request, Claim claim, ClaimNumber number, People people, Audit audit, RollupFigures before, Rollup rollup, ClaimLog log) {
        String event = switch (request.decision()) {
            case APPROVE -> "CLAIM_APPROVED";
            case REJECT -> "CLAIM_REJECTED";
            case NONE -> switch (claim.getStatus()) {
                case OPEN -> "CLAIM_OPENED";
                case PENDING -> "CLAIM_PENDING";
                case SETTLED -> "CLAIM_SETTLED";
                case VOID -> "CLAIM_VOIDED";
                case DRAFT -> throw new IllegalStateException("A claim never returns to DRAFT");
            };
        };
        net.officefloor.hq.app.person.Person person = people.find(claim.getSheetId(), claim.getPersonId());
        log.record(claim, person, request.user());
        String line = ClaimAudit.line(event, request.user(), person, claim, number.value());
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (request.decision() == ValidClaimStatus.Decision.REJECT) {
            line += " reason=" + request.reason();
        }
        audit.recordOnCommit(line);
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
