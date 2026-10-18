package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses a correction (a negative amount) unless the number it names is a SETTLED claim of the same
 *  person on its sheet. */
public class CheckClaimCorrects {
    public void check(@Val Claim claim, ClaimRepository repository) throws InvalidClaimException {
        if (!claim.isCorrection()) {
            return;
        }
        boolean settled = claim.getCorrects() != null && repository
                .findFirstBySheetIdAndNumber(claim.getSheetId(), claim.getCorrects())
                .filter((c) -> c.getPersonId().equals(claim.getPersonId()) && c.getStatus() == ClaimStatus.SETTLED)
                .isPresent();
        if (!settled) {
            throw new InvalidClaimException("A correction must name a settled claim of the same person it corrects.");
        }
    }
}
