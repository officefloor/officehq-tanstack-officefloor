package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a review assignment: REVIEW_ASSIGNED naming the claim and the user. */
public class RecordReviewAssigned {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("REVIEW_ASSIGNED", by, claim, people)
                + " user=" + claim.getReviewAssignee());
    }
}
