package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward assigns a REVIEW claim to a user ({@code user}, trimmed). Refused unless the user is a
 *  steward, for a claim in any other status, and for a blank user. */
public class AssignReview {
    public void assign(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "user", required = false) String user, @Val Claim claim,
            RoleRepository roles) throws ClaimStatusException, InvalidClaimException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can assign a claim's review");
        }
        if (!ClaimStatus.REVIEW.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim's review cannot be assigned");
        }
        String to = user == null ? "" : user.trim();
        if (to.isEmpty()) {
            throw new InvalidClaimException("A user to assign the review to is required");
        }
        claim.setReviewAssignee(to);
    }
}
