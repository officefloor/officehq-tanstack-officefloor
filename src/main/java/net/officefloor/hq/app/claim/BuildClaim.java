package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim, due by its date plus the terms in force on that date of its person's grade on that date, fixed from now on. */
public class BuildClaim {
    public void build(@Val Person person, @Val ClaimInput input, ClaimRepository repository,
            SheetRepository sheets, SettingRepository settings, Out<Claim> claim) {
        LocalDate date = input.date() != null ? input.date() : sheets.today(person.getSheetId());
        LocalDate dueBy = date.plusDays(settings.dated(person.getSheetId()).terms(person.gradeOn(date), date));
        claim.set(repository.insert(person.getSheetId(), person.getId(), input.amount(), date, input.note(),
                input.corrects(), dueBy));
    }
}
