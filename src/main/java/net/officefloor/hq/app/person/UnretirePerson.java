package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses (400) to unretire a person who is not retired; otherwise unretires them on the sheet's today, keeping the
 * retirement as a past one.
 */
public class UnretirePerson {
    public void unretire(@Val Person person, PersonRepository people, SheetRepository sheets, Out<Person> updated)
            throws InvalidPersonException {
        if (!person.retired()) {
            throw new InvalidPersonException(person.name() + " is not retired.");
        }
        updated.set(people.unretire(person, sheets.today(person.sheetId())));
    }
}
