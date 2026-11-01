package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** A steward sets the due-by of a DRAFT, OPEN or PENDING claim directly. It stays as set: the claim
 *  keeps no terms, so a holiday added later does not re-fix it. Overdue, the late charge, grace and
 *  the claim's period follow the set date. Refused: anyone but a steward, a claim in any other
 *  status, or a claim due by (now or as set) in a closed period. */
public class ApplyClaimDueBySet {
    public void apply(@Val ClaimDueBySet change, @Val Claim claim, Settings settings, Sheets sheets,
            ClosedPeriods closedPeriods) throws ClaimStatusRefusedException, PeriodClosedException {
        String sheetId = claim.getSheetId();
        if (sheets.archived(sheetId) || settings.role(sheetId, change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can set a claim's due-by");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT && !claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only a DRAFT, OPEN or PENDING claim's due-by can be set");
        }
        closedPeriods.refuse(sheetId, claim.getDueBy());
        closedPeriods.refuse(sheetId, change.dueBy());
        change.set(claim.getDueBy());
        claim.setDueBy(change.dueBy());
        claim.setDueTerms(null);
    }
}
