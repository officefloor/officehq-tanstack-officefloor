package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Stores the new status. Opening gives the claim the sheet's next number. A change another user made first (the
 * stored status is no longer the one this change started from) is refused, and the transaction gives back the number.
 */
public class SaveClaimStatus {
    public void save(@Val Claim claim, ClaimRepository claims, Out<Claim> saved,
            Out<ClaimNumber> number) throws InvalidClaimStatusException {
        Claim toSave = claim;
        if (claim.status() == ClaimStatus.OPEN) {
            long next = claims.nextNumber(claim.sheetId());
            toSave = new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                    claim.note(), claim.status(), next);
            number.set(new ClaimNumber(next));
        }
        ClaimStatus from = switch (claim.status()) {
            case OPEN -> ClaimStatus.DRAFT;
            case SETTLED -> ClaimStatus.OPEN;
            default -> null;
        };
        boolean stored = from != null ? claims.updateStatus(toSave, from)
                : claims.updateStatus(toSave, ClaimStatus.DRAFT) || claims.updateStatus(toSave, ClaimStatus.OPEN);
        if (!stored) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(toSave);
    }
}
