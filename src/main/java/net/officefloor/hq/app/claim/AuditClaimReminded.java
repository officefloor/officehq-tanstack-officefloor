package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored reminder: REMIND sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>,
 *  then number=<n> when the claim has one. */
public class AuditClaimReminded {
    public void audit(@Val RemindInput input, @Val Claim claim, PersonStore people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("REMIND sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber()));
    }
}
