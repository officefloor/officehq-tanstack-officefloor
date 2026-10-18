package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the given date (none: the sheet's today), refusing one already retired. */
public class RetirePerson {
    public void retire(@Val RetireInput input, @Val Person person, SheetRepository sheets) throws InvalidPersonException {
        if (person.getRetiredFrom() != null) {
            throw new InvalidPersonException(person.getName() + " is already retired.");
        }
        person.setRetiredFrom(input.from() != null ? input.from()
                : sheets.findById(person.getSheetId()).orElseThrow().getToday());
    }
}
