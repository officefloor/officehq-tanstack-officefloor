package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses any change to a claim belonging to a closed period: one whose due-by falls inside it. */
public class RefuseClosedClaim {
    public void check(@Val Claim claim, PeriodRepository periods) throws BadRequestException {
        if (periods.closedOn(claim.getSheetId(), claim.getDueBy())) {
            throw new BadRequestException(RefuseClosedDate.CLOSED);
        }
    }
}
