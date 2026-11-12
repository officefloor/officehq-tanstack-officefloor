package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim added to a retired person dated after the date they are retired from. */
public class CheckRetired {
    public void check(@Val ClaimInput input, @Val Person person) throws InvalidSheetException {
        Retirement.refuseClaimDate(person, input.date());
    }
}
