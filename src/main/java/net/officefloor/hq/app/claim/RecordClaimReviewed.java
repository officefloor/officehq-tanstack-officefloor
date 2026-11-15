package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a review: CLAIM_REVIEWED (without the number), then CLAIM_OPENED with the number
 *  it was given. */
public class RecordClaimReviewed {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        String opened = RecordClaimStatus.line(ClaimStatus.OPEN.event(), by, claim, people);
        String reviewed = RecordClaimStatus.line("CLAIM_REVIEWED", by, claim, people);
        audit.record(reviewed.substring(0, reviewed.length() - claim.getAuditNumber().length()));
        audit.record(opened);
    }
}
