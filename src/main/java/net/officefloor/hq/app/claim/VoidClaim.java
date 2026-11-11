package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.voidreason.VoidReason;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Voids the claim. Only a role the permission matrix allows may void. A SETTLED claim may be
 * voided only by a steward, and only within {@link #SETTLED_DAYS} days of its settlement (its
 * status log's SETTLED date) by the sheet's today; it keeps its number and its frozen figure,
 * which leaves every total. While the sheet has void reasons, a void needs one of them, kept on
 * the claim.
 */
public class VoidClaim {
    /** How many days after its settlement a SETTLED claim may still be voided. */
    static final long SETTLED_DAYS = 7;

    public void apply(@Val Sheet sheet, @Val Claim claim, @Val VoidRequest body, RoleRepository roles,
            SettingsRepository settings, ClaimLogRepository log, VoidReasonRepository reasons,
            HttpServletRequest request) throws InvalidClaimStatusException, InvalidClaimException {
        voidClaim(sheet, claim, body.reason(), Audit.user(request), roles, settings, log, reasons);
    }

    /** Voids the claim with the reason (null: none) as the user, by the rules above; refused, it is left as it was. */
    static void voidClaim(Sheet sheet, Claim claim, String reason, String user, RoleRepository roles,
            SettingsRepository settings, ClaimLogRepository log, VoidReasonRepository reasons)
            throws InvalidClaimStatusException, InvalidClaimException {
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
        java.util.List<String> listed = reasons.findAll(sheet.id()).stream().map(VoidReason::reason).toList();
        if (!listed.isEmpty()) {
            if (reason == null) throw new InvalidClaimException("A void needs a reason from the sheet's list");
            if (!listed.contains(reason)) throw new InvalidClaimException(reason + " is not on the sheet's void reasons");
        } else if (reason != null) {
            throw new InvalidClaimException("The sheet has no void reasons: " + reason + " is not one");
        }
        claim.moveTo(ClaimStatus.VOID);
        claim.setVoidReason(reason);
    }
}
