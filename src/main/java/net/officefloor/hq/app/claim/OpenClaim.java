package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens a claim, giving it its number (see {@link ClaimNumbers}) and recording who opened it.
 *  Refused for a role the permission matrix does not let open. */
public class OpenClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            SheetRepository sheets, RoleRepository roles)
            throws ClaimStatusException, PeriodClosedException {
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.OPEN, roles, settings)) {
            throw new ClaimStatusException("Your role may not open a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.OPEN).name());
        claim.setNumber(ClaimNumbers.next(claim, claims, periods, settings, sheets));
        claim.setOpenedBy(by == null ? "" : by);
    }
}
