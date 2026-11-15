package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class AddClaim {
    public void add(@RequestParam(name = "by", required = false) String by,
            @Val ValidClaim request, @Val Person person,
            ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            HolidayRepository holidays, Out<Claim> claim)
            throws PeriodClosedException, InvalidClaimException {
        String user = by == null ? "" : by;
        LocalDate date = request.date() != null ? request.date() : settings.entryDate(person.getSheetId());
        if (person.retiredBefore(date)) {
            throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
        }
        // The due-by is fixed now: the person's own terms, else their grade's, else the sheet's, as they stood on its date,
        // moved past the sheet's holidays as they are now.
        // The claim belongs to the period its due-by falls in.
        LocalDate dueBy = holidays.dueBy(person.getSheetId(),
                date.plusDays(settings.terms(person.getSheetId(), person, date.toString())));
        periods.requireOpen(person.getSheetId(), dueBy.toString());
        String corrects = Corrections.check(request, person.getSheetId(), person.getId(), claims);
        Claim added = claims.add(person.getSheetId(), person.getId(), request.amount(), date,
                request.note(), corrects, dueBy, user);
        if (periods.softClosedOn(person.getSheetId(), dueBy.toString())) claims.markLateEntry(added);
        claim.set(added);
    }
}
