package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the added claim, once it is saved, and its late entry when it is one. */
public class AuditClaimAdded {
    public void audit(@Val Claim claim, @Val Person person,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        ClaimRules.auditAdded(claim, person.getName(), AuditUser.decode(user), audit);
    }
}
