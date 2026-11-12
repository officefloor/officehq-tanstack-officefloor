package net.officefloor.hq.app.quick;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimPeriods;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a quick add whose claim would be due by a date inside a closed period: its date plus the terms
 *  of the person named (or, for a person still to be added, of their grade). */
public class CheckQuickAddPeriod {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, @Val ClaimInput claim,
            PersonStore people, SettingStore settings, PeriodStore periods, HolidayStore holidays) throws InvalidSheetException {
        String sheet = sheetId.trim();
        Person person = people.findByName(sheet, request.name());
        LocalDate dueBy = person != null ? ClaimPeriods.dueByOf(person, claim.date(), settings, holidays)
                : holidays.nextWorkingDay(sheet, claim.date().plusDays(settings.terms(sheet, request.grade(), claim.date())));
        ClaimPeriods.refuseClosed(periods, sheet, dueBy);
    }
}
