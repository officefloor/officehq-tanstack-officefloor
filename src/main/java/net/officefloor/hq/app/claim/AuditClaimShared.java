package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the share, once it is saved: the claim's line, then with=<holder> share=<share>. */
public class AuditClaimShared {
    public void audit(@Val Claim claim, @Val ShareRequest request, @Val Person holder, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_SHARED", claim, person, AuditUser.decode(user))
                + " with=" + holder.getName() + " share=" + request.share());
    }
}
