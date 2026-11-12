package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses an edit typing another amount than a claim with lines has: its amount is the sum of its lines. */
public class CheckClaimLines {
    public void check(@Val ClaimInput input, @Val Claim claim, ClaimLineStore lines) throws InvalidSheetException {
        if (input.amount().compareTo(claim.getAmount()) != 0 && lines.has(claim)) {
            throw new InvalidSheetException("A claim with lines takes its amount from its lines");
        }
    }
}
