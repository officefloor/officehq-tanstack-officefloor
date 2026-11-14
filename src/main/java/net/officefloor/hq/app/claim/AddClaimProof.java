package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one, and its count of the proof's kind. */
public class AddClaimProof {
    public void add(@Val Claim claim, @Val ProofKind kind, ClaimRepository repository, Out<Claim> proved) {
        Claim next = claim.withProof();
        repository.saveProofs(next);
        if (ProofKind.PHOTO.equals(kind.stored())) {
            repository.addPhotoProof(next.sheetId(), next.id());
        }
        proved.set(next);
    }
}
