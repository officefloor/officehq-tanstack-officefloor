package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository repository) throws StalePersonException {
        if (!repository.save(person)) throw new StalePersonException(String.valueOf(person.getId()));
    }
}
