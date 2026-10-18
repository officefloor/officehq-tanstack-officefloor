package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records CLAIM_REASSIGNED once the claim is saved, naming it (with its new person) and ending with the names
 *  it moved from and to; the signed-in user arrives as X-User. */
public class AuditClaimReassigned {
    public void audit(@Val Claim claim, @Val Reassignment reassignment, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REASSIGNED", claim, user, people)
                + " from=" + reassignment.getFromName() + " to=" + reassignment.getToName());
    }
}
