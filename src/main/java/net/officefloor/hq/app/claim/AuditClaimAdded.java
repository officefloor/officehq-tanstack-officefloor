package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_ADDED once the claim is saved; the signed-in user arrives as X-User. */
public class AuditClaimAdded {
    public void audit(@Val Claim claim, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_ADDED", claim, user, people));
    }
}
