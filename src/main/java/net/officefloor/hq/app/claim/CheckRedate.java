package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim's re-dating unless a steward re-dates an OPEN or PENDING claim to a date not after the
 *  date its person is retired from. Its due-by, and so its period, stays as it is, so the new date may fall in
 *  a closed period (a claim due by a date in one is refused before this). */
public class CheckRedate {
    public void check(@Val RedateInput input, @Val Claim claim, RoleStore roles, PersonStore people) throws InvalidSheetException {
        if (roles.find(claim.getSheetId(), input.user()) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may re-date a claim");
        }
        if (claim.getStatus() != ClaimStatus.OPEN && claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidSheetException("A " + claim.getStatus() + " claim cannot be re-dated");
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null) {
            Retirement.refuseClaimDate(person, input.date());
        }
    }
}
