package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Refuses a new correction that does not name a SETTLED claim of its person. */
public class CheckClaimAddCorrection {
    public void check(@Val ValidClaim request, @Val Person person, ClaimRepository claims) throws InvalidClaimException {
        Correction.refuse(request, person.getSheetId(), person.getId(), claims);
    }
}
