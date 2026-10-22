package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Store the new claim under the person; with no date given it is dated the sheet's today. Its due-by is fixed now:
 *  its date plus the terms of the person's grade in force on its date (else the sheet's terms). */
public class SaveNewClaim {
    public void save(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            ClaimRepository repository, Out<Claim> claim) {
        var date = input.date() != null ? input.date() : sheets.today(person.sheetId());
        var dueBy = date.plusDays(settings.termsFor(person.sheetId(), person.grades().on(date)));
        claim.set(repository.insert(new Claim(0, person.sheetId(), person.id(), input.amount(), date, input.note(),
                ClaimStatus.DRAFT, null, null, null, false, null, input.corrects(), dueBy)));
    }
}
