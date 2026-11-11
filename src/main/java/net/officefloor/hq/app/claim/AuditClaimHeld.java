package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the claim held (CLAIM_HELD) or released (CLAIM_RELEASED), naming the claim like every claim line. */
public class AuditClaimHeld {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people, HttpServletRequest request,
            Audit audit) {
        Person person = people.find(sheet.id(), claim.getPersonId());
        audit.record(ClaimAudit.line(claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED", sheet, Audit.user(request),
                person, claim));
    }
}
