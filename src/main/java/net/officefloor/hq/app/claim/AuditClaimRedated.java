package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored re-date in the audit file, naming the claim like every claim line (with its new date and
 *  number), then its old and new date. */
public class AuditClaimRedated {
    public void audit(@Val RedateInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        ClaimRedate.audit(input, loaded, saved, people, audit);
    }
}
