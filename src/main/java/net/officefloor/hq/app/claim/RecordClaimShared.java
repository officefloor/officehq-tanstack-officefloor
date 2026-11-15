package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a share: CLAIM_SHARED naming the claim, ending with its new holder and their share. */
public class RecordClaimShared {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val ClaimShare share, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_SHARED", by, claim, people)
                + " with=" + share.name() + " share=" + share.share());
    }
}
