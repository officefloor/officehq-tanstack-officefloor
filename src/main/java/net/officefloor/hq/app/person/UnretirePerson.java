package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Unretires a retired person, keeping the retirement as a past one; refuses anyone not retired. */
public class UnretirePerson {
    public void unretire(@Val UnretireInput input, @Val Person person, PersonStore store) throws InvalidSheetException {
        if (!person.isRetired()) {
            throw new InvalidSheetException(person.getName() + " is not retired");
        }
        store.unretire(person, input.on());
    }
}
