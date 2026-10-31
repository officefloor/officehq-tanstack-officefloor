package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A steward sets the due-by of a DRAFT, OPEN or PENDING claim directly. The due-by stays as set: the claim no longer
 * keeps the terms it was fixed with, so a holiday added later does not move it. Overdue, the late charge, grace and
 * the claim's period follow it.
 */
public class ApplyDueBy {
    public void apply(@Val ValidDueBy request, @Val Claim claim, Settings settings, Out<ClaimDueBy> dueBy)
            throws ClaimStatusRefusedException {
        if (!Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()))) {
            throw new ClaimStatusRefusedException("Only a steward can set a claim's due-by");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT && claim.getStatus() != ClaimStatus.OPEN
                && claim.getStatus() != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("Only a DRAFT, OPEN or PENDING claim can have its due-by set");
        }
        dueBy.set(new ClaimDueBy(claim.getDueBy()));
        claim.setDueBy(request.dueBy());
        claim.setTerms(null);
    }
}
