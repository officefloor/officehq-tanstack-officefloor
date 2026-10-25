package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodRepository;

/** Refuses a claim whose due-by (the period it belongs to) falls inside a closed period. */
public class RefuseClosedDate {

    static final String CLOSED = "That claim's date is in a closed period";

    private RefuseClosedDate() {
    }

    static void refuse(String sheetId, LocalDate dueBy, PeriodRepository periods) throws BadRequestException {
        if (periods.closedOn(sheetId, dueBy)) {
            throw new BadRequestException(CLOSED);
        }
    }
}
