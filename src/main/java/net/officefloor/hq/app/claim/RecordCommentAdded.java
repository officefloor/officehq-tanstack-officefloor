package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** COMMENT_ADDED, naming the claim, then its text. */
public class RecordCommentAdded {
    public void record(@Val Claim claim, @Val ClaimComment comment, @Val ChangedBy by, PersonRepository people,
            Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("COMMENT_ADDED", claim, by, person == null ? "" : person.getName())
                + " text=" + comment.text());
    }
}
