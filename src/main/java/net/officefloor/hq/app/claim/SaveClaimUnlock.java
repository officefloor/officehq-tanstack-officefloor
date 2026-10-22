package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Release the user's lock on the loaded claim's editor; another user's lock is left as it is. */
public class SaveClaimUnlock {
    public void save(@Val ClaimLockInput input, @Val Claim loaded, ClaimRepository repository,
            Out<ClaimResponse> saved) {
        repository.unlock(loaded, input.user());
        saved.set(ClaimResponse.from(loaded));
    }
}
