package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes REVIEW_ASSIGNED to the audit file once the assignment is stored, naming the claim, then user=<the assignee>. */
public class RecordReviewAssigned {
    public void record(@Val Claim claim, @Val ClaimReviewAssignment assignment, PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("REVIEW_ASSIGNED sheet=" + claim.sheetId() + " by=" + assignment.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + " user=" + assignment.user());
    }
}
