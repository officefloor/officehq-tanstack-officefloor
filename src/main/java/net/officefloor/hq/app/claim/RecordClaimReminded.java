package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes REMIND to the audit file once the reminder is stored, naming the claim (number=<number> when it has one), then
 *  ESCALATE naming the claim the same way when the reminder escalated it. */
public class RecordClaimReminded {
    public void record(@Val Claim claim, @Val ClaimReminder reminder, @Val ClaimEscalation escalation,
            PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        String named = " sheet=" + claim.sheetId() + " by=" + reminder.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number());
        audit.record("REMIND" + named);
        if (escalation.escalated()) {
            audit.record("ESCALATE" + named);
        }
    }
}
