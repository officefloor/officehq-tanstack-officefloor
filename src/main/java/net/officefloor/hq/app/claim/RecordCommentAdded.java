package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes COMMENT_ADDED to the audit file once the comment is stored, naming the claim (number=<number> when it has
 *  one) and ending with its text. */
public class RecordCommentAdded {
    public void record(@Val Claim claim, @Val ClaimComment comment, PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("COMMENT_ADDED sheet=" + claim.sheetId() + " by=" + comment.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number())
                + " text=" + comment.text());
    }
}
