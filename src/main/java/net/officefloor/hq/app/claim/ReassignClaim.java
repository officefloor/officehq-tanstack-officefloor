package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.UserRole;
import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** A steward moves a DRAFT, OPEN or PENDING claim to another person on its sheet; it keeps its number, date and
 *  status, and every total follows. Refused for a user who is not a steward (arriving as X-User), for a SETTLED
 *  or VOID claim, for a correction (it names a settled claim of its own person), for a person not on the sheet or
 *  the claim's own person, and when the claim is dated after the new person's retirement. */
public class ReassignClaim {
    public void apply(@Val Claim claim, @Val Reassignment reassignment, @HttpHeaderParameter("X-User") String user,
            PersonRepository people, UserRoleRepository roles)
            throws InvalidClaimStatusException, NotStewardException, InvalidClaimException, RetiredPersonException {
        String by = user == null ? "" : user.trim();
        if (!UserRole.STEWARD.equals(roles.roleOf(claim.getSheetId(), by))) {
            throw new NotStewardException("Only a steward can move a claim.");
        }
        if (claim.getStatus() == ClaimStatus.SETTLED || claim.getStatus() == ClaimStatus.VOID) {
            throw new InvalidClaimStatusException("A " + claim.getStatus() + " claim cannot be moved.");
        }
        if (claim.getCorrects() != null) {
            throw new InvalidClaimException("A correction cannot be moved to another person.");
        }
        Person to = people.findById(reassignment.getToPersonId())
                .filter((p) -> p.getSheetId().equals(claim.getSheetId())).orElse(null);
        if (to == null || to.getId().equals(claim.getPersonId())) {
            throw new InvalidClaimException("A claim must be moved to another person on its sheet.");
        }
        if (to.getRetiredFrom() != null && claim.getDate().isAfter(to.getRetiredFrom())) {
            throw new RetiredPersonException(to.getName() + " is retired from " + to.getRetiredFrom()
                    + "; a claim dated after that cannot be moved to them.");
        }
        String from = people.findById(claim.getPersonId()).orElseThrow().getName();
        claim.setPersonId(to.getId());
        reassignment.moved(from, to.getName());
    }
}
