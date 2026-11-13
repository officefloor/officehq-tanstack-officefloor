package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a reassign unless a steward moves a DRAFT, OPEN or PENDING claim to another person on the sheet who
 * is not retired before the claim's date; publishes the move.
 */
public class CheckClaimReassign {
    public void check(@RequestParam("user") String user, @Val Claim claim, @Val ReassignRequest request,
            SettingRepository settings, PersonRepository people, Out<Reassignment> reassignment)
            throws InvalidClaimException {
        if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can move a claim to another person.");
        }
        if (claim.status() != ClaimStatus.DRAFT && !claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be moved.");
        }
        Person to = people.find(claim.sheetId(), request.personId());
        if (to == null) {
            throw new InvalidClaimException("No such person to move the claim to.");
        }
        if (to.id() == claim.personId()) {
            throw new InvalidClaimException("The claim is already " + to.name() + "'s.");
        }
        if (to.retiredBefore(claim.date())) {
            throw new InvalidClaimException(
                    to.name() + " is retired from " + to.retiredFrom() + ": no claim can be dated " + claim.date() + ".");
        }
        reassignment.set(new Reassignment(people.find(claim.sheetId(), claim.personId()), to));
    }
}
