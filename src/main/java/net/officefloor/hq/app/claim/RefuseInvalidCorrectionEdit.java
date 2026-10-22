package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse an edit into a correction unless the number it names is a SETTLED claim of the claim's person. */
public class RefuseInvalidCorrectionEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.corrects() != null
                && !repository.settledNumbered(loaded.sheetId(), loaded.personId(), input.corrects())) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person; there is no SETTLED claim "
                    + input.corrects() + " of this person.");
        }
    }
}
