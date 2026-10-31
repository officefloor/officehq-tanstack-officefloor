package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the line is saved, so a refused line writes no audit line. */
public class RecordClaimLineAdded {
    public void record(@Val ClaimLine line, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record("CLAIM_LINE_ADDED sheet=" + claim.getSheetId() + " by=" + line.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + " label=" + line.label() + " qty=" + line.qty() + " unit=" + line.unit().toPlainString());
    }
}
