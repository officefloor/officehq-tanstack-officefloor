package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository repository, Out<Person> saved) {
        saved.set(repository.save(person));
    }
}
