package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** A steward waives the late charge of an OPEN or PENDING claim: from then on it bears none, whatever
 *  its overdue state. Refused: anyone but a steward, a claim in any other status, or one already waived. */
public class ApplyClaimWaive {
    public void apply(@Val ClaimWaive change, @Val Claim claim, Settings settings, Sheets sheets)
            throws ClaimStatusRefusedException {
        String sheetId = claim.getSheetId();
        if (sheets.archived(sheetId) || settings.role(sheetId, change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can waive a claim's late charge");
        }
        if (!claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("Only an OPEN or PENDING claim's late charge can be waived");
        }
        if (claim.isWaived()) {
            throw new ClaimStatusRefusedException("The claim's late charge is already waived");
        }
        claim.setWaived(true);
    }
}
