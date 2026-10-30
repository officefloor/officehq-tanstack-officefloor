package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;

/** Waives the late charge of an OPEN or PENDING claim; only a steward waives, and a claim only once. */
public class ApplyWaive {
    public void apply(@Val WaiveRequest request, @Val Claim claim, Settings settings) throws ClaimStatusRefusedException {
        if (!Settings.STEWARD.equals(settings.role(claim.getSheetId(), request.user()))) {
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
