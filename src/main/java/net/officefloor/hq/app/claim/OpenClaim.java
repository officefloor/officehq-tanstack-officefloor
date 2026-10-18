package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Moves the claim to OPEN, refusing it when its status does not allow that, and gives it its number (the
 *  sheet's next, or the next in its period while period numbering is on) and records who opened it (the
 *  signed-in user, arriving as X-User). */
public class OpenClaim {
    public void apply(@Val Claim claim, @HttpHeaderParameter("X-User") String user, ClaimRepository repository,
            SettingRepository settings, SheetRepository sheets, PeriodRepository periods) throws InvalidClaimStatusException {
        ClaimTransition.check(claim.getStatus(), ClaimStatus.OPEN);
        // Holding the numbers holds the sheet's numbers until this change commits or is refused. Then
        // re-check the stored status: an open of the same claim that got there first has committed by
        // now, so this one is refused (and its number rolls back with it).
        ClaimNumbering numbering = ClaimNumbering.hold(claim.getSheetId(), repository, settings, sheets);
        ClaimTransition.check(ClaimStatus.valueOf(repository.storedStatus(claim.getId())), ClaimStatus.OPEN);
        ClaimTransition.to(claim, ClaimStatus.OPEN);
        numbering.number(claim, repository, periods);
        claim.setOpenedBy(user == null ? "" : user.trim());
    }
}
