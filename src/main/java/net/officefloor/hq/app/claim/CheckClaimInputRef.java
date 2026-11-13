package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) a new claim whose external reference another claim on the sheet already has, ignoring case. */
public class CheckClaimInputRef {
    public void check(@Val ClaimInput input, @Val Person person, ClaimRepository claims) throws InvalidClaimException {
        if (input.ref() != null && claims.refUsed(person.sheetId(), input.ref(), null)) {
            throw new InvalidClaimException("The reference " + input.ref() + " is already used on the sheet.");
        }
    }
}
