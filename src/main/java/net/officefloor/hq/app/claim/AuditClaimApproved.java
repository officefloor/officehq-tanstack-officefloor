package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_APPROVED once the claim is saved, ending with its fixed due (n/a when no rate applied);
 *  the signed-in user arrives as X-User. */
public class AuditClaimApproved {
    public void audit(@Val Claim claim, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        String due = ClaimResponse.from(claim).settledDue();
        audit.record(ClaimAudit.line("CLAIM_APPROVED", claim, user, people) + " due=" + (due == null ? "n/a" : due));
    }
}
