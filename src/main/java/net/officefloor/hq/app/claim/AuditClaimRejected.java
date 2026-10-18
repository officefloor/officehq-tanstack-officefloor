package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_REJECTED once the claim is saved, ending with the reason; the signed-in user arrives as X-User. */
public class AuditClaimRejected {
    public void audit(@Val Claim claim, @Val Rejection rejection, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REJECTED", claim, user, people) + " reason=" + rejection.reason());
    }
}
