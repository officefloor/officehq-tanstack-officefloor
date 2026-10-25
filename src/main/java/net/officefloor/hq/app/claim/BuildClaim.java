package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim (date blank: the sheet's entry date, else its today), due by the terms-th business day after its date (skipping Saturdays, Sundays and the sheet's holidays), with the terms in force on that date for its person (their own, else their grade's on that date, else the sheet's) fixed from now on; a late entry when that due-by is in a soft-closed period. */
public class BuildClaim {
    public void build(@Val Person person, @Val ClaimInput input, ClaimRepository repository,
            SheetRepository sheets, SettingRepository settings, PeriodRepository periods, HolidayRepository holidays, Out<Claim> claim) {
        LocalDate date = input.date() != null ? input.date()
                : settings.entryDate(person.getSheetId(), sheets.today(person.getSheetId()));
        Claim added = repository.insert(person.getSheetId(), person.getId(), input.amount(), date, input.note(),
                input.corrects(), terms(person, date, settings));
        if (periods.softClosedOn(person.getSheetId(), added.getDueBy())) {
            repository.markLate(added);
        }
        claim.set(added);
    }

    /** The terms a claim of the person dated {@code date} is fixed with: those in force then for them. */
    static int terms(Person person, LocalDate date, SettingRepository settings) {
        return settings.dated(person.getSheetId()).terms(person, date);
    }

    /**
     * The due-by a claim of the person dated {@code date} would have: the terms-th business day after its date, by the
     * sheet's holidays as they stand now.
     */
    static LocalDate dueBy(Person person, LocalDate date, SettingRepository settings, HolidayRepository holidays) {
        return holidays.dueBy(person.getSheetId(), date, terms(person, date, settings));
    }
}
