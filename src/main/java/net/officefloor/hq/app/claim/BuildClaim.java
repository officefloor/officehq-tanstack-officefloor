package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim (date blank: the sheet's entry date, else its today), due by the terms-th business day after its date (skipping Saturdays, Sundays and the sheet's holidays), with the terms in force for its person on the day it is created, the sheet's today (their own, else their grade's on that day, else the sheet's) fixed from now on; a late entry when that due-by is in a soft-closed period. */
public class BuildClaim {
    public void build(@Val Person person, @Val ClaimInput input, ClaimRepository repository,
            SheetRepository sheets, SettingRepository settings, PeriodRepository periods, HolidayRepository holidays, Out<Claim> claim) {
        LocalDate today = sheets.today(person.getSheetId());
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(person.getSheetId(), today);
        Claim added = repository.insert(person.getSheetId(), person.getId(), input.amount(), date, input.note(),
                input.corrects(), input.ref(), input.tags(), terms(person, today, settings));
        if (periods.softClosedOn(person.getSheetId(), added.getDueBy())) {
            repository.markLate(added);
        }
        claim.set(added);
    }

    /**
     * The terms a claim of the person created on {@code today} (the sheet's today then) is fixed with: those in force
     * for them on that day, whatever the claim's date.
     */
    static int terms(Person person, LocalDate today, SettingRepository settings) {
        return settings.dated(person.getSheetId()).terms(person, today);
    }

    /**
     * The due-by a claim of the person dated {@code date}, created on {@code today}, would have: the terms-th business
     * day after its date, with the terms in force on {@code today}, by the sheet's holidays as they stand now.
     */
    static LocalDate dueBy(Person person, LocalDate date, LocalDate today, SettingRepository settings,
            HolidayRepository holidays) {
        return holidays.dueBy(person.getSheetId(), date, terms(person, today, settings));
    }
}
