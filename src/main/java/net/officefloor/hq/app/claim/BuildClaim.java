package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A new DRAFT claim, dated (when left undated) the sheet's entry date while it is set, else its today, its due-by fixed now: the terms-th business day after its date
 * (skipping Saturdays, Sundays and the holidays on the sheet now), its terms its person's own, else those of their grade on that date, else the sheet's.
 * Due by a date inside a soft-closed period (its period is the one containing its due-by), it is a late entry.
 */
public class BuildClaim {
    public void build(@Val ClaimInput input, @Val Sheet sheet, @Val Person person, SettingsRepository settings,
            PeriodRepository periods, HolidayRepository holidays,
            Out<Claim> claimOut) {
        Settings terms = settings.find(sheet.id());
        LocalDate date = input.date() == null ? terms.entryDay(sheet.today()) : input.date();
        Claim claim = new Claim(null, sheet.id(), person.getId(), input.amount(), date, input.note(),
                ClaimStatus.DRAFT, null, null, null);
        claim.setCorrects(input.corrects());
        claim.setDueBy(holidays.dueBy(sheet.id(), date, terms.termsFor(person, date)));
        claim.setLateEntry(periods.softClosedOn(sheet.id(), claim.getDueBy()) != null);
        claimOut.set(claim);
    }
}
