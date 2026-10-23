package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse an edit typing an amount other than the lines' total on a claim with lines: its amount is their total. */
public class RefuseLinedAmountEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, ClaimLineRepository lines)
            throws InvalidClaimException {
        var total = lines.total(loaded);
        if (total != null && total.compareTo(input.amount()) != 0) {
            throw new InvalidClaimException("This claim has lines; its amount is their total, "
                    + total.toPlainString() + ", and cannot be typed.");
        }
    }
}
