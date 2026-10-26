package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class RefuseUsedRefInput {
    // A claim being added may not carry a reference another claim of the sheet already carries (ignoring case).
    public void refuse(@Val ClaimInput input, @Val Person person, Claims claims) throws InvalidClaimException {
        RefuseUsedRef.refuse(input.ref(), person.sheetId(), 0, claims);
    }
}
