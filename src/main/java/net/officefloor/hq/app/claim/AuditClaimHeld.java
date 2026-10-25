package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the hold or the release, once it is saved: the claim's line, CLAIM_HELD or CLAIM_RELEASED. */
public class AuditClaimHeld {
    public void audit(@Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line(claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED", claim, person, AuditUser.decode(user)));
    }
}
