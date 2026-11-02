package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward assigns a REVIEW claim to a user to review: REVIEW_ASSIGNED is written naming the claim and ending
 * user=<user>. Refuses a blank user, a claim of any other status, and anyone but a steward.
 */
public class AssignReview {
    public void assign(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody AssignReviewRequest request, @Val Claim claim, Claims claims, People people, Roles roles,
            Audit audit) throws InvalidClaimStatusChangeException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward assigns a review");
        }
        if (claim.status() != ClaimStatus.REVIEW) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim is not assigned for review");
        }
        String to = request == null || request.user() == null ? "" : request.user().trim();
        if (to.isEmpty()) {
            throw new InvalidClaimStatusChangeException("Name the user to review the claim");
        }
        // Conditional on the status, so a concurrent review is refused.
        if (!claims.assignReview(sheet, claim.id(), to)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        Person person = people.find(sheet, claim.person());
        audit.record("REVIEW_ASSIGNED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " user=" + to);
    }
}
