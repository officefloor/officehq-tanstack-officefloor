package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Fails when the person is not on the sheet. */
public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            PersonRepository repository, Out<Person> person) throws PersonNotFoundException {
        Person found = null;
        try {
            found = repository.find(sheetId.trim(), Long.parseLong(personId.trim()));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (found == null) {
            throw new PersonNotFoundException(personId);
        }
        person.set(found);
    }
}
