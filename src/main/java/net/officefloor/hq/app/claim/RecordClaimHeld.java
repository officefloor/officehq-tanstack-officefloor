package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a hold toggle: CLAIM_HELD or CLAIM_RELEASED naming the claim. */
public class RecordClaimHeld {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line(claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED", by, claim, people));
    }
}
