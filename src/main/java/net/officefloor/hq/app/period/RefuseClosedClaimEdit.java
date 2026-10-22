package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse an edit of a claim dated inside a closed period, or one that would date it inside one. */
public class RefuseClosedClaimEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, SheetRepository sheets, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.date());
        if (closed == null) {
            var date = input.date() != null ? input.date() : sheets.today(loaded.sheetId());
            closed = periods.closedOn(loaded.sheetId(), date);
        }
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
