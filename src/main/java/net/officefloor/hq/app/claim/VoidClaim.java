package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Voids a claim; an opened claim keeps its number (still never reused). Refused for a role the
 *  permission matrix does not let void. A SETTLED claim may be voided only by a steward, within
 *  {@link #SETTLED_DAYS} days of its settlement (its status log's SETTLED date) by the sheet's today:
 *  its frozen figure leaves every total (it is kept on the claim, and reported as reversed). While the
 *  sheet has void reasons, a void needs one of them ({@code reason}), kept on the claim; refused without. */
public class VoidClaim {
    public static final int SETTLED_DAYS = 7;

    public void move(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "reason", required = false) String reason, @Val Claim claim,
            PeriodRepository periods, RoleRepository roles, SettingRepository settings,
            ClaimLogRepository log, SheetRepository sheets, VoidReasonRepository reasons)
            throws ClaimStatusException, PeriodClosedException, InvalidClaimException {
        String chosen = reason == null ? "" : reason.trim();
        if (reasons.any(claim.getSheetId())) {
            if (chosen.isEmpty() || !reasons.contains(claim.getSheetId(), chosen)) {
                throw new InvalidClaimException("A void needs a reason from the sheet's void reasons");
            }
            claim.setVoidReason(chosen);
        }
        if (ClaimStatus.SETTLED.name().equals(claim.getStatus())) {
            voidSettled(by, claim, periods, roles, log, sheets);
            return;
        }
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.VOID, roles, settings)) {
            throw new ClaimStatusException("Your role may not void a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.VOID).name());
    }

    private static void voidSettled(String by, Claim claim, PeriodRepository periods, RoleRepository roles,
            ClaimLogRepository log, SheetRepository sheets) throws ClaimStatusException, PeriodClosedException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by == null ? "" : by))) {
            throw new ClaimStatusException("Only a steward can void a SETTLED claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        LocalDate settledOn = log.settledOn(claim.getSheetId(), claim.getId());
        if (settledOn == null || sheets.today(claim.getSheetId()).isAfter(settledOn.plusDays(SETTLED_DAYS))) {
            throw new ClaimStatusException("A SETTLED claim can be voided only within " + SETTLED_DAYS
                    + " days of its settlement");
        }
        claim.setStatus(ClaimStatus.VOID.name());
        claim.setReversal(true);
    }
}
