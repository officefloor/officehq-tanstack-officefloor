package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a stored claim add: CLAIM_ADDED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>,
 *  followed for a late entry by LATE_ENTRY naming the claim the same way. */
public class AuditClaimAdded {
    public void audit(@Val ClaimInput input, @Val Person person, @Val Claim claim, @Val Rollup before, Audit audit,
            RollupService rollups) {
        record(input, person, claim, before, audit, rollups);
    }

    static void record(ClaimInput input, Person person, Claim claim, Rollup before, Audit audit,
            RollupService rollups) {
        audit.record(AuditClaimEdited.line("CLAIM_ADDED", input, person, claim));
        if (claim.isLateEntry()) {
            audit.record(AuditClaimEdited.line("LATE_ENTRY", input, person, claim));
        }
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
