package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Releases the user's lock on the claim, as its editor is cancelled; another user's lock stays. */
public class ApplyUnlock {
    public void apply(@RequestBody LockRequest request, @Val Claim claim, ClaimStore store) {
        store.unlock(claim, request == null || request.user() == null ? "" : request.user().trim());
    }
}
