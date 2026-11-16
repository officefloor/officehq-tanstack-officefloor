package net.officefloor.hq.app.quickadd;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.ClaimRules;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonValues;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** The claim is dated the sheet's entry date, else its today; the due-by it would be fixed with (by the named person's terms, else a
 *  new person's: grade A's) may not be inside a closed period. Checked before a new person is added, so a refused claim leaves no
 *  person and no audit record behind. */
public class CheckQuickAddDate {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonValues values, SettingRepository settings,
            PeriodRepository periods, PersonRepository people) throws InvalidClaimException {
        String sheet = sheetId.trim();
        LocalDate date = settings.claimDate(sheet);
        Person found = people.findByName(sheet, values.name());
        LocalDate dueBy = found != null ? ClaimRules.dueBy(found, date, settings)
                : date.plusDays(settings.terms(sheet, values.terms(), values.grade(), date));
        if (periods.closedOn(sheet, dueBy)) {
            throw new InvalidClaimException("The claim's due-by is inside a closed period");
        }
    }
}
