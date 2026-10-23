package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a new claim whose due-by (its date — a blank date being the sheet's entry date, else its today — plus the
 *  terms it would get) falls inside a period closed by hand (one closed automatically does not refuse it): a claim belongs to the period holding its due-by, as a pasted or
 *  template claim does. A claim whose date and due-by straddle a closed period (it would run across it: the period
 *  starts after its date and ends before its due-by) is refused too. */
public class RefuseClosedNewClaim {
    public void refuse(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(person.sheetId());
        var date = input.date() != null ? input.date() : sheets.newClaimDate(person.sheetId());
        var dueBy = DueBy.of(person, date, settings, sheets, holidays);
        Period closed = periods.closedByHandFor(person.sheetId(), date, dueBy);
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
