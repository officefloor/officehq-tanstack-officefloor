package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse the quick-add as the claim form refuses a new claim: when the claim's due-by (its date — the sheet's entry
 *  date, else its today — plus the terms it would get: the named person's, else those of a new grade A person) falls
 *  inside a period closed by hand, or it would run across one — before any person is added, so a refused quick-add
 *  leaves nothing behind. */
public class RefuseClosedQuickAdd {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, SheetRepository sheets,
            PersonRepository people, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods)
            throws ClosedPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        var date = sheets.newClaimDate(id);
        Person found = people.findByName(id, request.name());
        int terms = found != null ? DueBy.terms(found, settings, sheets) : settings.termsFor(id, request.grade(), sheets.today(id));
        Period closed = periods.closedByHandFor(id, date, holidays.dueBy(id, date, terms));
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
