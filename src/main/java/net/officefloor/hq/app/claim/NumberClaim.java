package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Gives a claim being opened the sheet's next number; refused when another request opened it first. */
public class NumberClaim {
    public void number(@Val Claim claim, ClaimRepository repository) throws InvalidClaimStatusException {
        if (!repository.markOpened(claim)) throw new InvalidClaimStatusException(ClaimStatus.DRAFT, ClaimStatus.OPEN);
        claim.setNumber(repository.nextNumber(claim.getSheetId()));
    }
}
