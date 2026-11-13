package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, PersonRepository repository, Out<Person> saved) throws StalePersonException {
        Person stored = repository.save(person);
        if (stored == null) {
            throw new StalePersonException(person.name());
        }
        saved.set(stored);
    }
}
