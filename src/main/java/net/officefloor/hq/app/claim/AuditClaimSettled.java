package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_SETTLED once the claim is saved, ending with its fixed due (n/a when no rate applied), or
 *  CLAIM_PENDING when a clerk's settling sent it to a steward instead; the signed-in user arrives as X-User. */
public class AuditClaimSettled {
    public void audit(@Val Claim claim, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        if (claim.getStatus() == ClaimStatus.PENDING) {
            audit.record(ClaimAudit.line("CLAIM_PENDING", claim, user, people));
            return;
        }
        String due = ClaimResponse.from(claim).settledDue();
        audit.record(ClaimAudit.line("CLAIM_SETTLED", claim, user, people) + " due=" + (due == null ? "n/a" : due));
    }
}
