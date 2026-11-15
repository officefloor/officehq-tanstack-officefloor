package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a reminder that went through, then its escalation, if it escalated the claim. */
public class RecordClaimReminded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("REMIND", by, claim, people));
        if (claim.isEscalating()) {
            audit.record(RecordClaimStatus.line("ESCALATE", by, claim, people));
        }
    }
}
