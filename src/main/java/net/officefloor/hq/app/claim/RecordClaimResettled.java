package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a re-settle: CLAIM_RESETTLED, the claim line ending with its old and new fixed due. */
public class RecordClaimResettled {
    public void record(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            @Val Resettlement resettlement, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_RESETTLED", by, claim, people)
                + " old=" + (resettlement.old() == null ? "n/a" : resettlement.old())
                + " new=" + (resettlement.now() == null ? "n/a" : resettlement.now()));
    }
}
