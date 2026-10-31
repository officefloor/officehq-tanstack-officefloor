package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused status change writes no audit line. */
public class RecordClaimStatusChanged {
    public void record(@Val ClaimStatusChange change, @Val Claim claim, @Val ClaimNumber number, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(change.action().event() + " sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (number.value() != null ? " number=" + number.value() : ""));
    }
}
