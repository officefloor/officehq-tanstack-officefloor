package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository repository) {
        repository.saveAndFlush(person); // flushed so the response carries the raised version
    }
}
