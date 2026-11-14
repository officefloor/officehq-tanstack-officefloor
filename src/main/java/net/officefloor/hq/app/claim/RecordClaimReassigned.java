package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a reassignment: CLAIM_REASSIGNED naming the claim (with its new person), then
 *  the person it left and the one it went to. */
public class RecordClaimReassigned {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val Reassignment reassignment, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_REASSIGNED", by, claim, people)
                + " from=" + reassignment.from() + " to=" + reassignment.to());
    }
}
