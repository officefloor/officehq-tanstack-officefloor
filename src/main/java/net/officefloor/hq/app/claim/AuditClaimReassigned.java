package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored move in the audit file, naming the claim like every claim line (with its new person), then
 *  the person it moved from and to. */
public class AuditClaimReassigned {
    public void audit(@Val ReassignInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        ClaimReassign.audit(input, loaded, saved, people, audit);
    }
}
