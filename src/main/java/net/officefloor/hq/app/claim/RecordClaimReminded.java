package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Writes REMIND to the audit file once the reminder is stored, naming the claim (number=<number> when it has one). */
public class RecordClaimReminded {
    public void record(@Val Claim claim, @Val ClaimReminder reminder, PersonRepository people, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("REMIND sheet=" + claim.sheetId() + " by=" + reminder.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number()));
    }
}
