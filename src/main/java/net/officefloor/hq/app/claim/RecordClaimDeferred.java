package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a deferral that went through, ending with the date it is deferred until. */
public class RecordClaimDeferred {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_DEFERRED", by, claim, people)
                + " until=" + claim.getDeferredUntil());
    }
}
