package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AuditReviewAssigned {
    public void audit(@Val ReviewAssignRequest request, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("REVIEW_ASSIGNED", request.user(), person, claim, claim.getNumber())
                + " user=" + claim.getReviewUser());
    }
}
