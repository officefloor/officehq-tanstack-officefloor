package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * A correction must name, by number, a SETTLED claim of the same person; any other claim names
 * none.
 */
public class CheckCorrection {
    public void check(@Val Claim claim, ClaimRepository repository) throws InvalidClaimException {
        String corrects = claim.getCorrects();
        if (corrects == null) return;
        Claim corrected = repository.findByPerson(claim.getSheetId(), claim.getPersonId()).stream()
                .filter((c) -> corrects.equals(c.getNumber()) && c.getStatus() == ClaimStatus.SETTLED)
                .findFirst().orElse(null);
        if (corrected == null) {
            throw new InvalidClaimException("A correction names a settled claim of the same person");
        }
        // A correction rounds by the date of the claim it corrects.
        claim.setCorrectedDate(corrected.getDate());
    }
}
