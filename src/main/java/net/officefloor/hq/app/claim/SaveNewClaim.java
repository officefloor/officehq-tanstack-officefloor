package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Store the new claim under the person; with no date given it is dated the sheet's entry date, else its today. Its terms are fixed now:
 *  the terms in force on the sheet's today (the person's own, else their grade's, else the sheet's); its due-by is
 *  the terms-th business day after its date, skipping Saturdays, Sundays and the sheet's holidays. Its DRAFT line starts its status log. Due by inside a soft-closed period, it is a late entry. */
public class SaveNewClaim {
    public void save(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods, ClaimRepository repository, ClaimLogRepository log, Out<Claim> claim) {
        claim.set(insert(input, person, sheets, settings, holidays, periods, repository, log));
    }

    /** Store one new claim as the claim form does (the paste box stores each of its lines this way). */
    static Claim insert(ClaimInput input, Person person, SheetRepository sheets, SettingRepository settings, HolidayRepository holidays,
            PeriodRepository periods, ClaimRepository repository, ClaimLogRepository log) {
        var date = input.date() != null ? input.date() : sheets.newClaimDate(person.sheetId());
        int terms = DueBy.terms(person, date, settings, sheets);
        var dueBy = holidays.dueBy(person.sheetId(), date, terms);
        Claim added = repository.insert(terms, new Claim(0, person.sheetId(), person.id(), input.amount(), date, input.note(),
                ClaimStatus.DRAFT, null, null, null, false, null, input.corrects(), dueBy, null, 0, null,
                periods.softClosedOn(person.sheetId(), dueBy) != null, null, input.ref(), input.tags() == null || input.tags().isEmpty() ? null : input.tags(), false, 0, false));
        log.record(added, input.user());
        return added;
    }
}
