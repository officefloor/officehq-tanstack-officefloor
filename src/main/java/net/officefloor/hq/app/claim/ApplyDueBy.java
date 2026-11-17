package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Sets a DRAFT, OPEN or PENDING claim's due-by directly, by a steward. Overdue, the late charge, grace and the claim's
 * period follow the due-by as set. Anyone but a steward, any other claim, or a claim whose due-by (old or new) falls
 * inside a closed period, is refused.
 */
public class ApplyDueBy {
    public void apply(@Val Claim claim, @Val ClaimDueBySetting request, RoleRepository roles, PeriodRepository periods,
            Out<Claim> changed, Out<ClaimDueBySetting> setting) throws InvalidClaimStatusException {
        String sheet = claim.sheetId();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheet, request.by()))) {
            throw new InvalidClaimStatusException("Only a steward sets a claim's due-by");
        }
        if (claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.OPEN
                && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim's due-by cannot be set");
        }
        if (periods.closedOn(sheet, claim.dueBy()) || periods.closedOn(sheet, request.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        changed.set(new Claim(claim.id(), sheet, claim.personId(), claim.amount(), claim.date(), claim.note(),
                claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(),
                claim.corrects(), request.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom(), claim.ref(),
                claim.tags(), claim.held()));
        setting.set(new ClaimDueBySetting(request.dueBy(), request.by(), claim.dueBy()));
    }
}
