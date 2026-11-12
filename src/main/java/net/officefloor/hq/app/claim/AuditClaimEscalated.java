package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a reminder that escalated its claim, after its REMIND: ESCALATE sheet=<id> by=<user> person=<name>
 *  amount=<0.00> date=<yyyy-mm-dd>, then number=<n> when the claim has one. */
public class AuditClaimEscalated {
    public void audit(@Val RemindInput input, @Val Claim claim, @Val Escalation escalation, PersonStore people, Audit audit) {
        if (!escalation.escalated()) {
            return;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("ESCALATE sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber()));
    }
}
