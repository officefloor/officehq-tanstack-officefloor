package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, People people) throws PersonConflictException {
        if (!people.save(person)) {
            throw new PersonConflictException(person.getId());
        }
    }
}
