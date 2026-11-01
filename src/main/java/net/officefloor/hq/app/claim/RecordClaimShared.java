package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the share is saved, so a refused share writes no audit line. */
public class RecordClaimShared {
    public void record(@Val ClaimShareChange change, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Person with = people.find(claim.getSheetId(), change.share().personId());
        audit.record("CLAIM_SHARED sheet=" + claim.getSheetId() + " by=" + change.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + " with=" + with.getName() + " share=" + change.share().share().toPlainString());
    }
}
