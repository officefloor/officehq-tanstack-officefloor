package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Voids a claim; an opened claim keeps its number (still never reused). Refused for a role the
 *  permission matrix does not let void. */
public class VoidClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings)
            throws ClaimStatusException, PeriodClosedException {
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.VOID, roles, settings)) {
            throw new ClaimStatusException("Your role may not void a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.VOID).name());
    }
}
