package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class RefuseInvalidCorrectionInput {
    // A correction being added must name, by number, a SETTLED claim of the person it is added to.
    public void refuse(@Val ClaimInput input, @Val Person person, Claims claims) throws InvalidClaimException {
        RefuseInvalidCorrection.refuse(input, person.sheetId(), person.id(), claims);
    }
}
