package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored review assignment: REVIEW_ASSIGNED sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<yyyy-mm-dd> user=<assignee>. */
public class AuditReviewAssigned {
    public void audit(@Val AssignReviewInput input, @Val Claim claim, PersonStore people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("REVIEW_ASSIGNED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + " user=" + input.assignee());
    }
}
