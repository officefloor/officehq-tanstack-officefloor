package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim's move unless a steward moves a DRAFT, OPEN or PENDING claim that is not a
 *  correction to another person on the sheet, who is not retired from before the claim's date. */
public class CheckReassign {
    public void check(@Val ReassignInput input, @Val Claim claim, RoleStore roles, PersonStore people,
            Out<Reassignment> reassignment) throws InvalidSheetException {
        if (roles.find(claim.getSheetId(), input.user()) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can move a claim");
        }
        if (!claim.getStatus().canMove()) {
            throw new InvalidSheetException("A " + claim.getStatus() + " claim cannot move");
        }
        if (claim.isCorrection()) {
            throw new InvalidSheetException("A correction stays with the claim it corrects");
        }
        Person from = people.find(claim.getSheetId(), claim.getPersonId());
        Person to = people.find(claim.getSheetId(), input.to());
        if (to == null) {
            throw new InvalidSheetException("No such person: " + input.to());
        }
        if (to.getId() == claim.getPersonId()) {
            throw new InvalidSheetException("The claim is already " + to.getName() + "'s");
        }
        Retirement.refuseClaimDate(to, claim.getDate());
        reassignment.set(new Reassignment(from, to));
    }
}
