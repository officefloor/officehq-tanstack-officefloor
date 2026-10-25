package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim, due by its date plus the terms in force on that date for its person (their own, else their grade's on that date, else the sheet's), fixed from now on; a late entry when its date is in a soft-closed period. */
public class BuildClaim {
    public void build(@Val Person person, @Val ClaimInput input, ClaimRepository repository,
            SheetRepository sheets, SettingRepository settings, PeriodRepository periods, Out<Claim> claim) {
        LocalDate date = input.date() != null ? input.date() : sheets.today(person.getSheetId());
        LocalDate dueBy = date.plusDays(settings.dated(person.getSheetId()).terms(person, date));
        Claim added = repository.insert(person.getSheetId(), person.getId(), input.amount(), date, input.note(),
                input.corrects(), dueBy);
        if (periods.softClosedOn(person.getSheetId(), date)) {
            repository.markLate(added);
        }
        claim.set(added);
    }
}
