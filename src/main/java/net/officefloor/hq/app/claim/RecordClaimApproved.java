package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of an approval: CLAIM_APPROVED with the due it was fixed at. */
public class RecordClaimApproved {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_APPROVED", by, claim, people) + RecordClaimStatus.due(claim));
    }
}
