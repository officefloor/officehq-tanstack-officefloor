package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;

/** No claim dated inside a closed period may be added, edited, opened, settled or voided. */
final class ClosedPeriods {

    private ClosedPeriods() {
    }

    static void refuse(PeriodRepository periods, String sheetId, LocalDate date) throws InvalidClaimException {
        var closed = periods.closedOn(sheetId, date);
        if (closed.isPresent()) throw new InvalidClaimException(message(closed.get().name()));
    }

    static String message(String period) {
        return "The period " + period + " is closed.";
    }
}
