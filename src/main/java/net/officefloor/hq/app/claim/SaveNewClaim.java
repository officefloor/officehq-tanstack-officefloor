package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Store the new claim under the person; with no date given it is dated the sheet's today. Its due-by is fixed now:
 *  its date plus the person's own terms (else the terms of the person's grade in force on its date (else the sheet's terms in force on its date)). Its DRAFT line starts its status log. */
public class SaveNewClaim {
    public void save(@Val ClaimInput input, @Val Person person, SheetRepository sheets, SettingRepository settings,
            ClaimRepository repository, ClaimLogRepository log, Out<Claim> claim) {
        claim.set(insert(input, person, sheets, settings, repository, log));
    }

    /** Store one new claim as the claim form does (the paste box stores each of its lines this way). */
    static Claim insert(ClaimInput input, Person person, SheetRepository sheets, SettingRepository settings,
            ClaimRepository repository, ClaimLogRepository log) {
        var date = input.date() != null ? input.date() : sheets.today(person.sheetId());
        var dueBy = date.plusDays(person.terms() != null ? person.terms()
                : settings.termsFor(person.sheetId(), person.grades().on(date), date));
        Claim added = repository.insert(new Claim(0, person.sheetId(), person.id(), input.amount(), date, input.note(),
                ClaimStatus.DRAFT, null, null, null, false, null, input.corrects(), dueBy, null, 0, null));
        log.record(added, input.user());
        return added;
    }
}
