package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * A correction must name, by number, a SETTLED claim of the same person; any other claim names
 * none.
 */
public class CheckCorrection {
    public void check(@Val Claim claim, ClaimRepository repository) throws InvalidClaimException {
        Integer corrects = claim.getCorrects();
        if (corrects == null) return;
        boolean found = repository.findByPerson(claim.getSheetId(), claim.getPersonId()).stream()
                .anyMatch((c) -> corrects.equals(c.getNumber()) && c.getStatus() == ClaimStatus.SETTLED);
        if (!found) {
            throw new InvalidClaimException("A correction names a settled claim of the same person");
        }
    }
}
