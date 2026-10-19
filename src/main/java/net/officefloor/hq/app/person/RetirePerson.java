package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the requested date, or the sheet's today without one. */
public class RetirePerson {
    public void retire(@Val Retirement retirement, @Val Person person, SheetClock clock, PersonRepository repository) {
        person.setRetiredFrom(retirement.from() != null ? retirement.from() : clock.today(person.getSheetId()));
        repository.saveRetired(person);
    }
}
