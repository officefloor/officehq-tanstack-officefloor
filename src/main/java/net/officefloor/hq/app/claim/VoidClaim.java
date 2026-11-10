package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Voids the claim. Only a role the permission matrix allows may void. A SETTLED claim may be
 * voided only by a steward, and only within {@link #SETTLED_DAYS} days of its settlement (its
 * status log's SETTLED date) by the sheet's today; it keeps its number and its frozen figure,
 * which leaves every total.
 */
public class VoidClaim {
    /** How many days after its settlement a SETTLED claim may still be voided. */
    static final long SETTLED_DAYS = 7;

    public void apply(@Val Sheet sheet, @Val Claim claim, RoleRepository roles, SettingsRepository settings,
            ClaimLogRepository log, HttpServletRequest request) throws InvalidClaimStatusException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.find(sheet.id()).allows("void", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not void a claim");
        }
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            if (!Role.STEWARD.equals(role)) {
                throw new InvalidClaimStatusException("Only a steward may void a SETTLED claim: " + user + " is a " + role);
            }
            LocalDate settled = log.settledOn(claim);
            long days = settled == null ? -1 : ChronoUnit.DAYS.between(settled, sheet.today());
            if (days < 0 || days > SETTLED_DAYS) {
                throw new InvalidClaimStatusException("A SETTLED claim may be voided only within " + SETTLED_DAYS
                        + " days of its settlement" + (settled == null ? "" : " on " + settled));
            }
        }
        claim.moveTo(ClaimStatus.VOID);
    }
}
