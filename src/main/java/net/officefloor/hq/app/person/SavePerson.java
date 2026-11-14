package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository people) throws PersonConflictException {
        if (!people.update(person)) {
            throw new PersonConflictException(person.getId()); // saved by someone else meanwhile
        }
    }
}
