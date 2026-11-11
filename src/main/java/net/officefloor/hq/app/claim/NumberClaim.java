package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * Marks a claim being opened as opened; refused when another request opened it first. An opened claim
 * has no number yet: it is numbered when it is settled ({@link NumberSettledClaim}).
 */
public class NumberClaim {
    public void number(@Val Claim claim, ClaimRepository repository) throws InvalidClaimStatusException {
        if (!repository.markOpened(claim)) throw new InvalidClaimStatusException(ClaimStatus.DRAFT, ClaimStatus.OPEN);
    }
}
