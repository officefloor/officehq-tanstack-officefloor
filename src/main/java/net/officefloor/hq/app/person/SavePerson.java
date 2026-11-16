package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonNameException, StalePersonException {
        people.update(person, values.version());
    }
}
