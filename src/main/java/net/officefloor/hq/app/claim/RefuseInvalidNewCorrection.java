package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuse a new correction unless the number it names is a SETTLED claim of the same person, and it does not exceed
 *  what remains of that claim (its amount less the corrections already made against it, VOID or DELETED ones left out). */
public class RefuseInvalidNewCorrection {
    public void refuse(@Val ClaimInput input, @Val Person person, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        BigDecimal remains = repository.correctable(person.sheetId(), person.id(), input.corrects(), null);
        if (remains == null) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person; "
                    + person.name() + " has no SETTLED claim " + input.corrects() + ".");
        }
        if (input.amount().negate().compareTo(remains) > 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of " + input.corrects() + ": "
                    + remains.setScale(2) + ".");
        }
    }
}
