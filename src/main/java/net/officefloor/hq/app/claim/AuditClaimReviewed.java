package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records the review (without a number, as the claim stood in REVIEW), then its opening with the number
 * it was given. Runs after every rule has passed, so a refused review writes nothing.
 */
public class AuditClaimReviewed {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people, HttpServletRequest request,
            Audit audit) {
        String user = Audit.user(request);
        Person person = people.find(sheet.id(), claim.getPersonId());
        audit.record(ClaimAudit.unnumbered("CLAIM_REVIEWED", sheet, user, person, claim));
        audit.record(ClaimAudit.line(ClaimStatus.OPEN.auditEvent(), sheet, user, person, claim));
    }
}
