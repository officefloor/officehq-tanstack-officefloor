package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordClaimStatus {
    public void record(@Val Claim claim, @Val ClaimNumber number, @Val ChangedBy by, @Val ClaimReview review,
            PersonRepository people, Audit audit) {
        String event = switch (claim.getStatus()) {
            case OPEN -> review.asked() ? "CLAIM_REJECTED" : "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> review.asked() ? "CLAIM_APPROVED" : "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> throw new IllegalStateException("A claim never returns to DRAFT");
        };
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String line = ClaimAudit.line(event, claim, by, person == null ? "" : person.getName(), number.value());
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.OPEN && review.asked()) {
            line += " reason=" + review.reason();
        }
        audit.record(line);
    }
}
