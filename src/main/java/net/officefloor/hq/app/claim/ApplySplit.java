package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes the split amount off the claim: it keeps the rest of its amount and loses any figure a statement bound it to.
 * Only an OPEN claim that is not deferred (see ClaimDue#deferred) nor whose due-by falls inside a closed period is split, only by
 * an amount more than 0.00 and less than its amount, and only by a user whose role the sheet lets open a claim (the
 * new claim is theirs to open; see SettingRepository#permits).
 */
public class ApplySplit {
    public void apply(@Val Claim claim, @Val ClaimSplit split, SheetRepository sheets, PeriodRepository periods,
            SettingRepository settings, RoleRepository roles, Out<Claim> changed) throws InvalidClaimStatusException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusException("Only an OPEN claim can be split");
        }
        if (ClaimDue.deferred(claim, sheets.today(claim.sheetId()))) {
            throw new InvalidClaimStatusException("A deferred claim cannot be split");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        if (split.amount().signum() <= 0 || split.amount().compareTo(claim.amount()) >= 0) {
            throw new InvalidClaimStatusException("A split takes more than 0.00 and less than the claim's amount");
        }
        String role = roles.roleOf(claim.sheetId(), split.by());
        if (!settings.permits(claim.sheetId(), "open", role)) {
            throw new InvalidClaimStatusException("A " + role + " may not open a claim");
        }
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount().subtract(split.amount()),
                claim.date(), claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), false, null,
                claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom()));
    }
}
