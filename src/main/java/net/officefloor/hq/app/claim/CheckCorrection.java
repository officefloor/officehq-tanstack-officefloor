package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import net.officefloor.plugin.variable.Val;

/**
 * A correction must name, by number, a SETTLED claim of the same person; any other claim names
 * none. Nor may it exceed what remains of that claim: its amount less the corrections already made
 * against it (VOID or DELETED ones left out).
 */
public class CheckCorrection {
    public void check(@Val Claim claim, ClaimRepository repository) throws InvalidClaimException {
        String corrects = claim.getCorrects();
        if (corrects == null) return;
        List<Claim> claims = repository.findByPerson(claim.getSheetId(), claim.getPersonId());
        Claim corrected = claims.stream()
                .filter((c) -> corrects.equals(c.getNumber()) && c.getStatus() == ClaimStatus.SETTLED)
                .findFirst().orElse(null);
        if (corrected == null) {
            throw new InvalidClaimException("A correction names a settled claim of the same person");
        }
        // What remains: the target's amount plus the (negative) corrections already made against it.
        BigDecimal remaining = corrected.getAmount();
        for (Claim other : claims) {
            if (claim.getId() != null && Objects.equals(claim.getId(), other.getId())) continue;
            if (!corrects.equals(other.getCorrects())) continue;
            if (other.getStatus() == ClaimStatus.VOID || other.getStatus() == ClaimStatus.DELETED) continue;
            remaining = remaining.add(other.getAmount());
        }
        if (claim.getAmount().negate().compareTo(remaining) > 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of the claim it corrects");
        }
        // A correction rounds by the date of the claim it corrects.
        claim.setCorrectedDate(corrected.getDate());
    }
}
