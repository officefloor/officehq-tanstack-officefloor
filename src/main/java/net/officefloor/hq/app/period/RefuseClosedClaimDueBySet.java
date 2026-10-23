package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.DueBySetInput;
import net.officefloor.plugin.variable.Val;

/** Refuse setting the due-by of a claim whose due-by falls inside a closed period, or to a due-by inside one: the
 *  claim's period follows its due-by. */
public class RefuseClosedClaimDueBySet {
    public void refuse(@Val DueBySetInput input, @Val Claim loaded, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.dueBy());
        if (closed == null) {
            closed = periods.closedOn(loaded.sheetId(), input.dueBy());
        }
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
