package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            People people, Out<Person> personOut) throws PersonNotFoundException {
        Person person = null;
        try {
            person = people.find(sheetId, Long.parseLong(personId));
        } catch (NumberFormatException e) {
            // not an id this app assigned
        }
        if (person == null) {
            throw new PersonNotFoundException(personId);
        }
        personOut.set(person);
    }
}
