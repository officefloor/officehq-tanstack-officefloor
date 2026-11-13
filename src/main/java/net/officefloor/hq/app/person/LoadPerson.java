package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            PersonRepository repository, Out<Person> person) throws PersonNotFoundException {
        Person found = repository.find(sheetId, personId);
        if (found == null) {
            throw new PersonNotFoundException(personId);
        }
        person.set(found);
    }
}
