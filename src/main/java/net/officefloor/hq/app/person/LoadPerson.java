package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("personId") String personId, @Val Sheet sheet,
            PersonRepository repository, Out<Person> personOut) throws PersonNotFoundException {
        Person person = null;
        try {
            person = repository.find(sheet.id(), Long.parseLong(personId));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (person == null) throw new PersonNotFoundException(personId);
        personOut.set(person);
    }
}
