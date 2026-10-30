package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.plugin.variable.Val;

public class AuditClaimStatus {
    public void audit(@Val ValidClaimStatus request, @Val Claim claim, @Val ClaimNumber number, People people, Audit audit, @Val RollupFigures before, Rollup rollup, ClaimLog log, Watches watches) {
        record(request, claim, number, people, audit, before, rollup, log, watches);
    }

    /** Logs and audits a status change that went through, as a single change does. Bulk settle uses it too. */
    static void record(ValidClaimStatus request, Claim claim, ClaimNumber number, People people, Audit audit, RollupFigures before, Rollup rollup, ClaimLog log, Watches watches) {
        String event = switch (request.decision()) {
            case APPROVE -> "CLAIM_APPROVED";
            case REJECT -> "CLAIM_REJECTED";
            case REVIEW -> "CLAIM_OPENED";
            case NONE -> switch (claim.getStatus()) {
                case OPEN -> "CLAIM_OPENED";
                case PENDING -> "CLAIM_PENDING";
                case SETTLED -> "CLAIM_SETTLED";
                case VOID -> "CLAIM_VOIDED";
                case DELETED -> "CLAIM_DELETED";
                case DRAFT -> "CLAIM_RESTORED";
                case REVIEW -> "CLAIM_REVIEW";
            };
        };
        net.officefloor.hq.app.person.Person person = people.find(claim.getSheetId(), claim.getPersonId());
        // Read before this change's own line is logged.
        boolean voidedSettled = claim.getStatus() == ClaimStatus.VOID
                && ClaimStatus.SETTLED.name().equals(log.lastStatus(claim));
        log.record(claim, person, request.user());
        if (request.decision() == ValidClaimStatus.Decision.REVIEW) {
            // A steward's review: the review line, then the claim's opening with its number.
            audit.recordOnCommit(ClaimAudit.line("CLAIM_REVIEWED", request.user(), person, claim, null));
        }
        String line = ClaimAudit.line(event, request.user(), person, claim, number.value());
        if (voidedSettled) {
            // A voided SETTLED claim reverses its frozen figure out of every total.
            line += " reversed=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (request.decision() == ValidClaimStatus.Decision.REJECT) {
            line += " reason=" + request.reason();
        }
        if (claim.getStatus() == ClaimStatus.VOID && request.reason() != null) {
            line += " reason=" + request.reason();
        }
        audit.recordOnCommit(line);
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            // Settled or approved: each user watching the person is notified, after the claim's own line.
            watches.notifyWatchers(person, request.user(), number.value(), audit);
        }
        rollup.recordChanges(claim.getSheetId(), request.user(), before);
    }
}
