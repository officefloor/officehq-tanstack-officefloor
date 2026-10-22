package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuse a new correction unless the number it names is a SETTLED claim of the same person. */
public class RefuseInvalidNewCorrection {
    public void refuse(@Val ClaimInput input, @Val Person person, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.corrects() != null && !repository.settledNumbered(person.sheetId(), person.id(), input.corrects())) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person; "
                    + person.name() + " has no SETTLED claim " + input.corrects() + ".");
        }
    }
}
