package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** A person being edited may not take a name another person on the sheet already has. */
public class CheckPersonRename {
    public void check(@Val Person person, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonNameException {
        if (people.nameTaken(person.sheetId(), values.name(), person.id())) {
            throw new DuplicatePersonNameException(values.name());
        }
    }
}
