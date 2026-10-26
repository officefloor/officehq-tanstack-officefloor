package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_LINE_ADDED, naming the claim with its new amount, then the line's label, quantity and unit amount. */
public class RecordLineAdded {
    public void record(@Val Claim claim, @Val ClaimLine line, @Val ChangedBy by, PersonRepository people,
            Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_LINE_ADDED", claim, by, person == null ? "" : person.getName())
                + " label=" + line.label() + " qty=" + line.qty() + " unit=" + line.unit().toPlainString());
    }
}
