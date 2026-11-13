package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a hold to the person, from the date given (blank: the sheet's today) to its to-date (blank: no end); a
 * to-date before the from-date is refused (400).
 */
public class HoldPerson {
    public void hold(@Val Person person, @Val Hold hold, PersonRepository people, SheetRepository sheets,
            Out<Person> updated) throws InvalidPersonException {
        Hold dated = hold.from() != null ? hold : new Hold(sheets.today(person.sheetId()), hold.to());
        if (dated.to() != null && dated.to().isBefore(dated.from())) {
            throw new InvalidPersonException("A hold's to-date must not be before its from-date.");
        }
        updated.set(people.hold(person, dated));
    }
}
