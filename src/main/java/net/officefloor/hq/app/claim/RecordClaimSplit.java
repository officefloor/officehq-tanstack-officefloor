package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a split that went through, naming the original with its remaining amount and the
 *  new claim's number, then the new claim's opening. */
public class RecordClaimSplit {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Split split, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("CLAIM_SPLIT", by, split.original(), people)
                + " into=" + (split.created().getNumber() == null ? "" : split.created().getNumber()));
        audit.record(RecordClaimStatus.line(ClaimStatus.OPEN.event(), by, split.created(), people));
    }
}
