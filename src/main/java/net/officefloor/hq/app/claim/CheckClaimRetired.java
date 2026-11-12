package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim edited to a date after the date its person is retired from. */
public class CheckClaimRetired {
    public void check(@Val Claim claim, @Val ClaimInput input, PersonStore people) throws InvalidSheetException {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null) {
            Retirement.refuseClaimDate(person, input.date());
        }
    }
}
