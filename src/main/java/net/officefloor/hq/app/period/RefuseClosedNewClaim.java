package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a new claim dated inside a closed period, or whose due-by (its date — a blank date being the sheet's entry date,
 *  else its today — plus the terms it would get) falls inside one. */
public class RefuseClosedNewClaim {
    public void refuse(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(person.sheetId());
        var date = input.date() != null ? input.date() : sheets.newClaimDate(person.sheetId());
        Period closed = periods.closedOn(person.sheetId(), date);
        if (closed == null) {
            closed = periods.closedOn(person.sheetId(), DueBy.of(person, date, settings, holidays));
        }
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
