package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored status change: CLAIM_OPENED, CLAIM_SETTLED or CLAIM_VOIDED sheet=<id> by=<user>
 *  person=<name> amount=<0.00> date=<yyyy-mm-dd>, then number=<n> when the claim has or had a number. */
public class AuditClaimStatus {
    public void audit(@Val StatusInput input, @Val Claim claim, @Val StatusChange change, PersonStore people,
            Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record(input.status().event() + " sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (change.number() == null ? "" : " number=" + change.number()));
    }
}
