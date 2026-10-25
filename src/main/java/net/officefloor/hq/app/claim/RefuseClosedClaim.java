package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses any change to a claim dated inside a closed period. */
public class RefuseClosedClaim {
    public void check(@Val Claim claim, PeriodRepository periods) throws BadRequestException {
        if (periods.closedOn(claim.getSheetId(), claim.getDate())) {
            throw new BadRequestException(RefuseClosedDate.CLOSED);
        }
    }
}
