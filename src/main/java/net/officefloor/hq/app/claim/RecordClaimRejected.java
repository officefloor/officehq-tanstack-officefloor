package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a rejection: CLAIM_REJECTED with its reason. */
public class RecordClaimRejected {
    public void record(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "reason", required = false) String reason,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_REJECTED", by, claim, people) + " reason=" + reason.trim());
    }
}
