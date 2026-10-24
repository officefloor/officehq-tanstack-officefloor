package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the added claim, once it is saved. */
public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_ADDED", claim, person.getName(), AuditUser.decode(user)));
    }
}
