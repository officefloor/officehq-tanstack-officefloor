package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the reminder, once it is saved: the claim's line, then the claim's ESCALATE line when it escalated it. */
public class AuditClaimReminded {
    public void audit(@Val Claim claim, @Val Escalation escalation, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        String by = AuditUser.decode(user);
        audit.record(ClaimAudit.line("REMIND", claim, person, by));
        if (escalation.escalated()) {
            audit.record(ClaimAudit.line("ESCALATE", claim, person, by));
        }
    }
}
