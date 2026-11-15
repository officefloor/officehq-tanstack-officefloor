package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of a re-dating: CLAIM_REDATED naming the claim (with its new date and number), then
 *  the date it had and the one it has now. */
public class RecordClaimRedated {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val Redating redating, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_REDATED", by, claim, people)
                + " old=" + redating.oldDate() + " new=" + redating.newDate());
    }
}
