package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the line, once it is saved: the claim's line with its new amount, then the line's label, qty and unit. */
public class AuditLineAdded {
    public void audit(@Val LineInput line, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_LINE_ADDED", claim, person, AuditUser.decode(user)) + " " + line.audit());
    }
}
