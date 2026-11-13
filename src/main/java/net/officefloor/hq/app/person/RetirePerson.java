package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date given, or the sheet's today when it was left blank. */
public class RetirePerson {
    public void retire(@Val Person person, @Val Retirement retirement, PersonRepository people,
            SheetRepository sheets, Out<Person> updated) {
        updated.set(people.retire(person,
                retirement.from() != null ? retirement.from() : sheets.today(person.sheetId())));
    }
}
