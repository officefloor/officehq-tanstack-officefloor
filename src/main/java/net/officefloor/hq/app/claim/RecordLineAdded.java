package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_LINE_ADDED to the audit file once the line is stored, naming the claim with its new amount and ending
 *  with the line's label, quantity and unit amount. */
public class RecordLineAdded {
    public void record(@Val Claim claim, @Val ClaimLine line, PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_LINE_ADDED sheet=" + claim.sheetId() + " by=" + line.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + " label=" + line.label() + " qty=" + line.qty()
                + " unit=" + line.unit().toPlainString());
    }
}
