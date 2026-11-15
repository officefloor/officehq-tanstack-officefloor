package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Raises a claim's proofs count by one, of the kind chosen (receipt or photo; none chosen: a receipt).
 *  Refused for a kind that is neither, for a claim due by a day in a closed period, and for a SETTLED,
 *  VOID or DELETED claim. */
public class AddProof {
    public void add(@RequestParam(name = "kind", required = false) String kind, @Val Claim claim,
            PeriodRepository periods) throws InvalidClaimException, ClaimStatusException, PeriodClosedException {
        String chosen = kind == null || kind.isBlank() ? ProofKind.RECEIPT : kind.trim();
        if (!ProofKind.ALL.contains(chosen)) {
            throw new InvalidClaimException("A proof's kind is receipt or photo");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (ClaimStatus.SETTLED.name().equals(claim.getStatus()) || ClaimStatus.VOID.name().equals(claim.getStatus())
                || ClaimStatus.DELETED.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot gain a proof");
        }
        claim.setProofs(claim.getProofs() + 1);
        if (ProofKind.PHOTO.equals(chosen)) {
            claim.setPhotoProofs(claim.getPhotoProofs() + 1);
        }
    }
}
