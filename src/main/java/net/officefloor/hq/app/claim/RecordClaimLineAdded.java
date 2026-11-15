package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a line that was added, with the claim's new amount, ending with the line. */
public class RecordClaimLineAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val ClaimLine line, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_LINE_ADDED", by, claim, people)
                + " label=" + line.label() + " qty=" + line.qty() + " unit=" + line.unit().toPlainString());
    }
}
