package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the claim's due-by to the new one, kept as set: a holiday added later does not move it. */
public class SetClaimDueBy {
    public void set(@Val Claim claim, @Val DueBySetting setting, ClaimRepository repository, Out<Claim> changed) {
        repository.setDueBy(claim.sheetId(), claim.id(), setting.to());
        changed.set(repository.find(claim.sheetId(), claim.id()));
    }
}
