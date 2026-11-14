package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a waiver unless a steward waives an OPEN or PENDING claim whose late charge is not already waived. */
public class CheckClaimWaive {
    public void check(@RequestParam("user") String user, @Val Claim claim, SettingRepository settings)
            throws InvalidClaimException {
        if (settings.find(claim.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidClaimException("Only a steward can waive a claim's late charge.");
        }
        if (!claim.status().open()) {
            throw new InvalidClaimException("A " + claim.status() + " claim's late charge cannot be waived.");
        }
        if (claim.waived()) {
            throw new InvalidClaimException("The claim's late charge is already waived.");
        }
    }
}
