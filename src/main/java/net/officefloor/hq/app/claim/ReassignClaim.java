package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to its new person, keeping its number and date; it loses any statement figure. */
public class ReassignClaim {
    public void reassign(@Val Claim claim, @Val Reassignment reassignment, ClaimRepository repository,
            Out<Claim> moved) {
        moved.set(repository.reassign(claim, reassignment.to().id()));
    }
}
