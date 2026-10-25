package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the due-by set, once it is saved: the claim's line, then its old and new due-by. */
public class AuditClaimDueBySet {
    public void audit(@Parameter DueBySet set, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_DUE_BY_SET", claim, person, AuditUser.decode(user))
                + " old=" + set.oldDueBy() + " new=" + set.newDueBy());
    }
}
