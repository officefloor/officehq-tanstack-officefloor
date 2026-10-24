package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the moved claim, once it is saved, under its new person, with the names it moved from and to. */
public class AuditClaimReassigned {
    public void audit(@Parameter String from, @Val Claim claim, @Val Person to,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REASSIGNED", claim, to.getName(), AuditUser.decode(user))
                + " from=" + from + " to=" + to.getName());
    }
}
