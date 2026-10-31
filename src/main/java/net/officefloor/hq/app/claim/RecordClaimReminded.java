package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused reminder writes no audit line. */
public class RecordClaimReminded {
    public void record(@Val ClaimRemind change, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record("REMIND sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : ""));
    }
}
