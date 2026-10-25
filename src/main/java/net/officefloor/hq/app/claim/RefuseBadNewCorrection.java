package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses a new correction that does not name a SETTLED claim of its person, or a claim naming one that is not a correction. */
public class RefuseBadNewCorrection {
    public void check(@Val Person person, @Val ClaimInput input, ClaimRepository repository) throws BadRequestException {
        RefuseBadCorrection.refuse(input, person.getSheetId(), person.getId(), repository);
    }
}
