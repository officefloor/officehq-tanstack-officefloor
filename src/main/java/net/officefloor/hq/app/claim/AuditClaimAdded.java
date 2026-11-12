package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a stored claim add: CLAIM_ADDED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>. */
public class AuditClaimAdded {
    public void audit(@Val ClaimInput input, @Val Person person, @Val Claim claim, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record(AuditClaimEdited.line("CLAIM_ADDED", input, person, claim));
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
