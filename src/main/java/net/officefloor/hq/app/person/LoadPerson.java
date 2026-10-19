package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            PersonRepository repository, Out<Person> personOut) throws PersonNotFoundException {
        Person person = repository.findById(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        personOut.set(person);
    }
}
