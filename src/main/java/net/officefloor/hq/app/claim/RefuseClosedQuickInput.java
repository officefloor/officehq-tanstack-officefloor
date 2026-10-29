package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.quick.QuickName;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseClosedQuickInput {
    // As for a claim added through the form, before any person is added: the quick-added claim's due-by is its date (the
    // entry date, else the sheet's today) plus the terms in force on the sheet's today of the person of that name, or of the sheet's default grade for a new person.
    public void refuse(@PathVariable("sheetId") String sheetId, @Val ClaimInput input, @Val QuickName name,
            People people, Settings settings, Holidays holidays, Periods periods) throws ClaimInClosedPeriodException {
        String sheet = sheetId.trim();
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(sheet);
        Person person = people.findByName(sheet, name.name());
        int terms = person != null ? person.termsNow(settings) : settings.termsNow(sheet, settings.defaultGrade(sheet));
        RefuseClosedNewInput.check(periods, sheet, holidays.dueBy(sheet, date, terms));
    }
}
