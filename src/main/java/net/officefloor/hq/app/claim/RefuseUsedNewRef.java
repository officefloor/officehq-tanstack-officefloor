package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuse a new claim whose reference another claim on the sheet already has, compared without regard to case. */
public class RefuseUsedNewRef {
    public void refuse(@Val ClaimInput input, @Val Person person, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.ref() != null && repository.refUsed(person.sheetId(), input.ref(), 0)) {
            throw new InvalidClaimException(RefuseUsedNewRef.message(input.ref()));
        }
    }

    static String message(String ref) {
        return "The reference " + ref + " is already used on this sheet.";
    }
}
