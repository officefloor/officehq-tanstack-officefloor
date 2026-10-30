package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AuditClaimCommented {
    public void audit(@Val CommentRequest request, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("COMMENT_ADDED", request.user(), person, claim, claim.getNumber())
                + " text=" + request.text().trim());
    }
}
