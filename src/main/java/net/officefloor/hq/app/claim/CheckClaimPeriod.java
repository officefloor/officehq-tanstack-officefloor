package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a change to a claim dated inside a closed period: editing it or moving it on. */
public class CheckClaimPeriod {
    public void check(@Val Claim claim, PeriodStore periods) throws InvalidSheetException {
        refuseClosed(periods, claim.getSheetId(), claim.getDate());
    }

    static void refuseClosed(PeriodStore periods, String sheetId, LocalDate date) throws InvalidSheetException {
        PeriodResponse closed = periods.closedOn(sheetId, date);
        if (closed != null) {
            throw new InvalidSheetException("The period " + closed.name() + " is closed");
        }
    }
}
