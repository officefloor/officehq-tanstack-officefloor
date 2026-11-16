package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Unretires a retired person on the sheet's today, keeping the retirement as a past one; anyone not retired is refused. */
public class UnretirePerson {
    public void unretire(@Val Person person, SheetRepository sheets, PersonRepository people, Out<Person> unretired)
            throws InvalidPersonException {
        if (!person.retired()) {
            throw new InvalidPersonException(person.name() + " is not retired");
        }
        LocalDate on = sheets.today(person.sheetId());
        people.unretire(person.sheetId(), person.id(), LocalDate.parse(person.retiredFrom()), on);
        unretired.set(person.unretired(on));
    }
}
