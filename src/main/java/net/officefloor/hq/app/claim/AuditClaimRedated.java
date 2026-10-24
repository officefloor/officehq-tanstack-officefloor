package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the re-dating, once it is saved: the claim's line with its new date, then its old and new date. */
public class AuditClaimRedated {
    public void audit(@Parameter Redated redated, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_REDATED", claim, person, AuditUser.decode(user))
                + " old=" + redated.oldDate() + " new=" + redated.newDate());
    }
}
