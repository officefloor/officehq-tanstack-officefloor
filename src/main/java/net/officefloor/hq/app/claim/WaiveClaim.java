package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Waives an OPEN or PENDING claim's late charge on the sheet's today: it bears none from then on.
 *  Refused unless the user is a steward, for a claim in any other status, and for one already waived. */
public class WaiveClaim {
    public void waive(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            RoleRepository roles, SheetRepository sheets) throws ClaimStatusException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can waive a claim's late charge");
        }
        if (!ClaimStatus.isOpen(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim's late charge cannot be waived");
        }
        if (claim.isWaived()) {
            throw new ClaimStatusException("The claim's late charge was already waived");
        }
        claim.setWaivedOn(sheets.today(claim.getSheetId()).toString());
    }
}
