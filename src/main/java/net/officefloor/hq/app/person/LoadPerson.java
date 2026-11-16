package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a person of the sheet by id; one not on the sheet is a 404. */
public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            PersonRepository people, Out<Person> person) throws PersonNotFoundException {
        Person found;
        try {
            found = people.find(sheetId.trim(), Long.parseLong(personId.trim()));
        } catch (NumberFormatException e) {
            found = null;
        }
        if (found == null) {
            throw new PersonNotFoundException("No person " + personId);
        }
        person.set(found);
    }
}
