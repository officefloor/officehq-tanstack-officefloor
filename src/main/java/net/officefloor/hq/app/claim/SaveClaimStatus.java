package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            Out<ClaimResponse> saved) throws ClaimStatusNotAllowedException {
        if (!loaded.status().canBecome(input.to()) || !repository.changeStatus(loaded, input.to())) {
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), input.to())));
    }
}
