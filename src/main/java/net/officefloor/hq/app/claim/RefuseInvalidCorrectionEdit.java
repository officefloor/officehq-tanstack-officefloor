package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Val;

/** Refuse an edit into a correction unless the number it names is a SETTLED claim of the claim's person, and it does
 *  not exceed what remains of that claim (its amount less the other corrections made against it, VOID or DELETED ones left out). */
public class RefuseInvalidCorrectionEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        BigDecimal remains = repository.correctable(loaded.sheetId(), loaded.personId(), input.corrects(), loaded.id());
        if (remains == null) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person; there is no SETTLED claim "
                    + input.corrects() + " of this person.");
        }
        if (input.amount().negate().compareTo(remains) > 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of " + input.corrects() + ": "
                    + remains.setScale(2) + ".");
        }
    }
}
