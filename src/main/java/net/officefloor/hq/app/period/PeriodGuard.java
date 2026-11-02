package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;

/** Refuse a change to a claim dated inside a closed period. */
public final class PeriodGuard {
    private PeriodGuard() {
    }

    public static void open(PeriodRepository periods, String sheetId, LocalDate date) throws BadRequestException {
        String closed = periods.closedOn(sheetId, date);
        if (closed != null) {
            throw new BadRequestException("the period " + closed + " is closed");
        }
    }
}
