package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A clerk settles a claim only when the due it fixes is at or below the sheet's clerk limit (a due of n/a is
 * within any limit); a steward settles anything. Refused (400) otherwise.
 */
public class CheckClerkLimit {
    public void check(@Val ClaimAction action, @Val Claim claim, @RequestParam("user") String user,
            SettingRepository settings) throws InvalidClaimException {
        if (action == ClaimAction.SETTLE && !settings.find(claim.sheetId()).maySettle(user, claim.due())) {
            throw new InvalidClaimException("A clerk cannot settle a claim whose due is above the clerk limit.");
        }
    }
}
