package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim's new date (blank: the sheet's today) that falls inside a closed period. */
public class RefuseClosedDate {

    static final String CLOSED = "That claim's date is in a closed period";

    public void check(@Val SheetResponse sheet, @Val ClaimInput input, SheetRepository sheets,
            PeriodRepository periods) throws BadRequestException {
        LocalDate date = input.date() != null ? input.date() : sheets.today(sheet.id());
        refuse(sheet.id(), date, periods);
    }

    static void refuse(String sheetId, LocalDate date, PeriodRepository periods) throws BadRequestException {
        if (periods.closedOn(sheetId, date)) {
            throw new BadRequestException(CLOSED);
        }
    }
}
