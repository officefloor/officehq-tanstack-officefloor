package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Rejects a PENDING claim with a reason: it goes back to OPEN, still bound by its person's last
 *  statement when that statement bound it. Refused unless the user is a steward or the person's approver, and without a reason. */
public class RejectClaim {
    public void move(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "reason", required = false) String reason, @Val Claim claim,
            PeriodRepository periods, RoleRepository roles, PersonRepository people)
            throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (!ClaimStatus.PENDING.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be rejected");
        }
        String user = by == null ? "" : by;
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))
                && !ApproveClaim.isApprover(people.find(claim.getSheetId(), claim.getPersonId()), user)) {
            throw new ClaimStatusException("Only a steward or the person's approver can reject a claim");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidClaimException("A rejection needs a reason");
        }
        claim.setStatus(ClaimStatus.PENDING.moveTo(ClaimStatus.OPEN).name());
    }
}
