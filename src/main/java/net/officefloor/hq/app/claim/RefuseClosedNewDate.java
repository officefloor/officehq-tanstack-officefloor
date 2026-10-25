package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses a new claim's date (blank: the sheet's entry date, else its today) that falls inside a closed period. */
public class RefuseClosedNewDate {

    public void check(@Val SheetResponse sheet, @Val ClaimInput input, SheetRepository sheets,
            SettingRepository settings, PeriodRepository periods) throws BadRequestException {
        RefuseClosedDate.refuse(sheet.id(), input.date() != null ? input.date()
                : settings.entryDate(sheet.id(), sheets.today(sheet.id())), periods);
    }
}
