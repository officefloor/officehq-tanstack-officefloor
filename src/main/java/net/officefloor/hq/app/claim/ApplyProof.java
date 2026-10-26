package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Raises the claim's proofs count by one, and its count of the proof's kind; a SETTLED, VOID or DELETED claim is refused. */
public class ApplyProof {
    public void apply(@Val Claim claim, @Val ChosenProofKind kind, ClaimRepository repository) throws BadRequestException {
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID
                || claim.getStatus() == ClaimStatus.DELETED) {
            throw new BadRequestException("A " + claim.getStatus() + " claim takes no more proofs");
        }
        if (!repository.addProof(claim, kind.kind())) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
    }
}
