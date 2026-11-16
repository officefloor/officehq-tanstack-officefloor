package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new status: DRAFT to OPEN, OPEN to SETTLED, DRAFT or OPEN to VOID. Voiding clears the
 * claim's number; the number it had is kept for the audit line.
 */
public class ApplyClaimStatus {
    public void apply(@Val Claim claim, @Val ClaimStatusChange change, Out<Claim> changed, Out<ClaimNumber> number)
            throws InvalidClaimStatusException {
        boolean allowed = switch (change.to()) {
            case OPEN -> claim.status() == ClaimStatus.DRAFT;
            case SETTLED -> claim.status() == ClaimStatus.OPEN;
            case VOID -> claim.status() == ClaimStatus.DRAFT || claim.status() == ClaimStatus.OPEN;
            case DRAFT -> false;
        };
        if (!allowed) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be made " + change.to());
        }
        number.set(new ClaimNumber(claim.number()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), change.to(), change.to() == ClaimStatus.VOID ? null : claim.number()));
    }
}
