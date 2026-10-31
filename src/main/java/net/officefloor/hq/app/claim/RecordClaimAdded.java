package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Runs after the claim is saved, so a refused add writes no audit line and no log line. */
public class RecordClaimAdded {
    public void record(@Val ClaimValues values, @Val Person person, @Val Claim claim, ClaimLog log, Audit audit) {
        log.write(claim, values.user());
        audit.record("CLAIM_ADDED sheet=" + claim.getSheetId() + " by=" + values.user()
                + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                + " date=" + claim.getDate());
        if (claim.isLateEntry()) {
            audit.record("LATE_ENTRY sheet=" + claim.getSheetId() + " by=" + values.user()
                    + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                    + " date=" + claim.getDate());
        }
    }
}
