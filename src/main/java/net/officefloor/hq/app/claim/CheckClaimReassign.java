package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Refuses (400) a reassign unless a user whose role the permission matrix allows reassigning (standing rule: a steward) moves a DRAFT, OPEN or PENDING claim to another person on the sheet who
 * is not retired before the claim's date; publishes the move.
 * A bulk move takes a claim already that person's as moved (to the same person) instead of refusing it.
 */
public class CheckClaimReassign {
    public void check(@RequestParam("user") String user, @Val Claim claim, @Val ReassignRequest request,
            SettingRepository settings, PersonRepository people, Out<Reassignment> reassignment)
            throws InvalidClaimException {
        if (!settings.find(claim.sheetId()).may(user, Permissions.REASSIGN)) {
            throw new InvalidClaimException("The user's role is not allowed to move a claim to another person.");
        }
        if (claim.status() != ClaimStatus.DRAFT && !claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be moved.");
        }
        Person to = people.find(claim.sheetId(), request.personId());
        if (to == null) {
            throw new InvalidClaimException("No such person to move the claim to.");
        }
        if (to.id() == claim.personId() && !Boolean.TRUE.equals(request.bulk())) {
            throw new InvalidClaimException("The claim is already " + to.name() + "'s.");
        }
        if (to.retiredBefore(claim.date())) {
            throw new InvalidClaimException(
                    to.name() + " is retired from " + to.retiredFrom() + ": no claim can be dated " + claim.date() + ".");
        }
        reassignment.set(new Reassignment(people.find(claim.sheetId(), claim.personId()), to));
    }
}
