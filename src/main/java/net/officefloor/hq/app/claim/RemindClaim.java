package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Reminds an OPEN or PENDING claim on the sheet's today, once per day: any other status, or a
 * second reminder the same day, is refused and writes nothing.
 */
public class RemindClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository) throws InvalidClaimStatusException {
        claim.remind(sheet.today());
        if (!repository.remind(claim)) throw new InvalidClaimStatusException("The claim was already reminded today");
    }
}
