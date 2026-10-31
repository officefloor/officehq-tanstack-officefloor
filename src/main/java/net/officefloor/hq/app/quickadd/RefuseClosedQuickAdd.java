package net.officefloor.hq.app.quickadd;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.DueBy;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a claim whose due-by (its date plus the terms it would get) falls in a closed period
 *  before anything is saved, so no new person is left behind. */
public class RefuseClosedQuickAdd {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val ClaimValues values, @Val Person person,
            Settings settings, Holidays holidays, ClosedPeriods closedPeriods) throws PeriodClosedException {
        LocalDate date = values.date() != null ? values.date() : settings.claimDate(sheetId);
        closedPeriods.refuse(sheetId, DueBy.of(person, date, settings, holidays));
    }
}
