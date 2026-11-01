package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheet") String sheet, @PathVariable("person") long person, People people,
            Out<Person> loaded) throws PersonNotFoundException {
        Person found = people.find(sheet, person);
        if (found == null) {
            throw new PersonNotFoundException(person);
        }
        loaded.set(found);
    }
}
