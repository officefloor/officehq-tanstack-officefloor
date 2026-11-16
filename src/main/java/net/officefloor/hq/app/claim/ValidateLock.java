package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: who opens or closes the claim's editor (a blank name is refused). */
public class ValidateLock {
    public void validate(@RequestBody LockRequest request, Out<ClaimLock> lock) throws InvalidClaimStatusException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidClaimStatusException("Who opens the editor is required");
        }
        lock.set(new ClaimLock(by));
    }
}
