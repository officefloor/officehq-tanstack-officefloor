package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses setting a claim's due-by unless a steward sets it on a DRAFT, OPEN or PENDING claim to a date not in a
 *  closed period (the claim then belongs to the period the new due-by falls in; one due by a date in a closed
 *  period now is refused before this). */
public class CheckDueBy {
    public void check(@Val DueByInput input, @Val Claim claim, RoleStore roles, PeriodStore periods) throws InvalidSheetException {
        if (roles.find(claim.getSheetId(), input.user()) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may set a claim's due-by");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT && claim.getStatus() != ClaimStatus.OPEN
                && claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidSheetException("A " + claim.getStatus() + " claim's due-by cannot be set");
        }
        ClaimPeriods.refuseClosed(periods, claim.getSheetId(), input.dueBy());
    }
}
