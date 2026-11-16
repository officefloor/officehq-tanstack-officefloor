package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step of setting a claim's due-by: CLAIM_DUE_BY_SET naming the claim, then the due-by it
 *  had and the one it has now. */
public class RecordClaimDueBySet {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val DueBySetting setting, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_DUE_BY_SET", by, claim, people)
                + " old=" + setting.oldDueBy() + " new=" + setting.newDueBy());
    }
}
