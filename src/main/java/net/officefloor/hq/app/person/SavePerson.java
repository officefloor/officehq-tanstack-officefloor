package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository repository) throws PersonConflictException {
        // Someone saved the person between load and save.
        if (!repository.save(person)) throw new PersonConflictException();
    }
}
