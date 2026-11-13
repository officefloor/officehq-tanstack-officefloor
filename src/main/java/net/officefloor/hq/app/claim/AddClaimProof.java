package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one. */
public class AddClaimProof {
    public void add(@Val Claim claim, ClaimRepository repository, Out<Claim> proved) {
        Claim next = claim.withProof();
        repository.saveProofs(next);
        proved.set(next);
    }
}
