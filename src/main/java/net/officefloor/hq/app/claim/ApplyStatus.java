package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val Claim claim) throws InvalidSheetException {
        if (!claim.getStatus().canBecome(input.status())) {
            throw new InvalidSheetException(
                    "A " + claim.getStatus() + " claim cannot become " + input.status());
        }
        claim.setStatus(input.status());
    }
}
