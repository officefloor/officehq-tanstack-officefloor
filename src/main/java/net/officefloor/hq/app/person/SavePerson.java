package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository people) throws DuplicatePersonNameException {
        people.update(person);
    }
}
