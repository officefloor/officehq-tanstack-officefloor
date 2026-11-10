package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Opens the claim, recording the signed-in user who opened it. Only a role the permission matrix allows may open. */
public class OpenClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, SettingsRepository settings,
            HttpServletRequest request) throws InvalidClaimStatusException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.find(sheet.id()).allows("open", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not open a claim");
        }
        claim.moveTo(ClaimStatus.OPEN);
        claim.setOpenedBy(user);
    }
}
