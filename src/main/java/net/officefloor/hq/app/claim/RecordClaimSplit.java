package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after both claims are saved, so a refused split writes no audit line: the split, naming the
 *  original with its remaining amount, then the new claim's opening. */
public class RecordClaimSplit {
    public void record(@Val ClaimSplit split, @Val Claim claim, People people, ClaimLog log, Audit audit) {
        Claim into = split.into();
        log.write(into, split.user());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record("CLAIM_SPLIT sheet=" + claim.getSheetId() + " by=" + split.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : "")
                + " into=" + into.getNumber());
        audit.record(ClaimAction.OPEN.event() + " sheet=" + into.getSheetId() + " by=" + split.user()
                + " person=" + person.getName() + " amount=" + into.getAmount().toPlainString()
                + " date=" + into.getDate() + " number=" + into.getNumber());
    }
}
