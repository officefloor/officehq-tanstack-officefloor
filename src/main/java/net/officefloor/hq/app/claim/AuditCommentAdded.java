package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a kept comment: COMMENT_ADDED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>,
 *  then number=<n> when the claim has one, then text=<text>. */
public class AuditCommentAdded {
    public void audit(@Val CommentInput input, @Val Claim claim, PersonStore people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("COMMENT_ADDED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber()) + " text=" + input.text());
    }
}
