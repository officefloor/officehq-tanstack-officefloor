package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new date, keeping its due-by, its number and any statement figure. Its period is the one
 * containing its due-by, so a re-date never moves it to another period (nor another period numbering sequence).
 */
public class RedateClaim {
    public void redate(@Val Claim claim, @Val Redating redating, ClaimRepository repository, Out<Claim> redated) {
        Claim moved = claim.redateTo(redating.to(), claim.number());
        repository.redate(moved);
        redated.set(moved);
    }
}
