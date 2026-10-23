package net.officefloor.hq.app.template;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a claim from a template whose due-by (its date — the sheet's entry date, else its today — plus the terms it
 *  would get) falls inside a closed period. Its date itself may fall in one: a template claim is never given a date. */
public class RefuseClosedTemplateClaim {
    public void refuse(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(person.sheetId());
        var date = sheets.newClaimDate(person.sheetId());
        Period closed = periods.closedOn(person.sheetId(), DueBy.of(person, date, settings, sheets, holidays));
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
