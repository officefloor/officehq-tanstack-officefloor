package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record the stored add in the audit file, with the amount and date as the screen shows them. */
public class AuditClaimAdded {
    public void audit(@Val ClaimInput input, @Val Person person, @Val Claim claim, Audit audit) {
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_ADDED sheet=" + claim.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + shown.amount() + " date=" + shown.date());
    }
}
