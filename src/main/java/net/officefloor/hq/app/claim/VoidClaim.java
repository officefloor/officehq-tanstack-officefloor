package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Voids the claim. Only a role the permission matrix allows may void. */
public class VoidClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, SettingsRepository settings,
            HttpServletRequest request) throws InvalidClaimStatusException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("void", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not void a claim");
        }
        claim.moveTo(ClaimStatus.VOID);
    }
}
