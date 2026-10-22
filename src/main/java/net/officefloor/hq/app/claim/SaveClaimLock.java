package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Lock the loaded claim's editor for the user: refused while another user holds it. The user's own lock is taken
 *  again. */
public class SaveClaimLock {
    public void save(@Val ClaimLockInput input, @Val Claim loaded, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!repository.lock(loaded, input.user())) {
            throw new ClaimStatusNotAllowedException("This claim is being edited by "
                    + repository.lockedBy(loaded) + "; it cannot be edited until they save or cancel.");
        }
        saved.set(ClaimResponse.from(loaded));
    }
}
