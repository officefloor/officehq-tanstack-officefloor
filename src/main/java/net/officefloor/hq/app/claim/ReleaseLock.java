package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Closes the claim's editor without saving (cancel): releases the claim's lock when the user holds it. */
public class ReleaseLock {
    public void release(@Val Claim claim, ClaimRepository repository,
            @RequestHeader(name = "X-User", required = false) String user) {
        String by = AuditUser.decode(user);
        if (by.equals(claim.getLockedBy())) {
            claim.setLockedBy(null);
            repository.saveAndFlush(claim);
        }
    }
}
