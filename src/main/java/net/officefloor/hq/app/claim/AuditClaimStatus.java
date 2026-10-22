package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored status change in the audit file, with the amount and date as the screen shows them,
 *  and the claim's number (for a void, the number it had); a settle or approve adds the due it fixed
 *  (n/a when the grade had no rate), a reject its reason. A settle that left the claim PENDING is CLAIM_PENDING. */
public class AuditClaimStatus {
    public void audit(@Val ClaimStatusInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        ClaimStatusChange.audit(input, loaded, saved, people, audit);
    }
}
