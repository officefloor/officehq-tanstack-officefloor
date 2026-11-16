package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_ADDED to the audit file once the claim is stored. */
public class RecordClaimAdded {
    public void record(@Val Claim claim, @Val Person person, @Val ClaimValues values, Audit audit) {
        audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + values.by() + " person=" + person.name()
                + " amount=" + claim.amount().toPlainString() + " date=" + claim.date());
    }
}
