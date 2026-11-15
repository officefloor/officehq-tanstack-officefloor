package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Unretires a retired person on the sheet's today, keeping the retirement as a past one; anyone
 *  not retired is refused and nothing is written. */
public class UnretirePerson {
    public void unretire(@Val Person person, SheetRepository sheets, PersonRepository people)
            throws InvalidPersonException {
        if (!person.isRetired()) {
            throw new InvalidPersonException("The person is not retired");
        }
        person.unretire(sheets.today(person.getSheetId()).toString());
        people.updateUnretired(person);
    }
}
