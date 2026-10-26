package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordClaimStatus {
    public void record(@Val Claim claim, @Val ClaimNumber number, @Val ChangedBy by, @Val ClaimReview review,
            PersonRepository people, WatchRepository watches, ClaimLogRepository logged, Audit audit, ClaimLog log) {
        String event = switch (claim.getStatus()) {
            case OPEN -> review.asked() ? "CLAIM_REJECTED" : "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> review.asked() ? "CLAIM_APPROVED" : "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DELETED -> "CLAIM_DELETED";
            case DRAFT -> "CLAIM_RESTORED";
            case REVIEW -> "CLAIM_REVIEW";
        };
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (review.release()) {
            // A steward's review: reviewed (as yet unnumbered), then opened with its number.
            audit.record(ClaimAudit.line("CLAIM_REVIEWED", claim, by, person == null ? "" : person.getName(), null));
        }
        String line = ClaimAudit.line(event, claim, by, person == null ? "" : person.getName(), number.value());
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.VOID && claim.getSettledDue() != null) {
            line += " reversed=" + claim.getSettledDue().setScale(2).toPlainString();
        } else if (claim.getStatus() == ClaimStatus.VOID
                && logged.lastOn(claim.getSheetId(), claim.getId(), ClaimStatus.SETTLED) != null) {
            // A voided SETTLED claim settled without a figure (its due was n/a) reverses none.
            line += " reversed=n/a";
        }
        if (claim.getStatus() == ClaimStatus.OPEN && review.asked()) {
            line += " reason=" + review.reason();
        }
        if (claim.getStatus() == ClaimStatus.VOID && claim.getVoidReason() != null) {
            line += " reason=" + claim.getVoidReason();
        }
        audit.record(line);
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            ClaimNotify.watchers(claim, person, number.value(), by, watches, audit);
        }
        log.record(claim, by);
    }
}
