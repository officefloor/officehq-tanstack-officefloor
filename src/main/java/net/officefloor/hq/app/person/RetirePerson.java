package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date asked (the sheet's today when none): they count as retired at once. */
public class RetirePerson {
    public void retire(@Val RetireRequest request, @Val Person person, SheetRepository sheets,
            PersonRepository people) {
        person.setRetiredFrom(request.from() != null ? request.from()
                : sheets.today(person.getSheetId()).toString());
        people.updateRetired(person);
    }
}
