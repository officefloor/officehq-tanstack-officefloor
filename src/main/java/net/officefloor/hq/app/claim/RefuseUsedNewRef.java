package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses a new claim whose reference a claim on the sheet already carries. */
public class RefuseUsedNewRef {
    public void check(@Val Person person, @Val ClaimInput input, ClaimRepository repository) throws BadRequestException {
        RefuseUsedRef.refuse(person.getSheetId(), input.ref(), -1, repository);
    }
}
