package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * Marks a reviewed claim as reviewed; refused when another request reviewed or voided it first. It has
 * no number yet: it is numbered when it is settled ({@link NumberSettledClaim}).
 */
public class NumberReviewedClaim {
    public void number(@Val Claim claim, ClaimRepository repository) throws InvalidClaimStatusException {
        if (!repository.markReviewed(claim)) throw new InvalidClaimStatusException(ClaimStatus.REVIEW, ClaimStatus.OPEN);
    }
}
