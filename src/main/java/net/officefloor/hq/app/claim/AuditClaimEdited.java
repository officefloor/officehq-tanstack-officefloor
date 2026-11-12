package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored claim edit: CLAIM_EDITED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>. */
public class AuditClaimEdited {
    public void audit(@Val ClaimInput input, @Val Claim claim, PersonStore people, Audit audit) {
        audit.record(line("CLAIM_EDITED", input, people.find(claim.getSheetId(), claim.getPersonId()), claim));
    }

    static String line(String event, ClaimInput input, Person person, Claim claim) {
        ClaimResponse shown = ClaimResponse.from(claim);
        return event + " sheet=" + claim.getSheetId() + " by=" + input.user() + " person=" + person.getName()
                + " amount=" + shown.amount() + " date=" + shown.date();
    }
}
