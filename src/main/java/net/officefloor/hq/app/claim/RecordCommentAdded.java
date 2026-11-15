package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: a comment that was added, ending with its text. */
public class RecordCommentAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, @Val ClaimComment comment, PersonRepository people, Audit audit) {
        audit.record(RecordClaimStatus.line("COMMENT_ADDED", by, claim, people) + " text=" + comment.text());
    }
}
