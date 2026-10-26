package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            People people, Out<Person> personOut) throws PersonNotFoundException {
        Person person = people.find(sheetId.trim(), personId);
        if (person == null) {
            throw new PersonNotFoundException("No person " + personId + " on this sheet.");
        }
        personOut.set(person);
    }
}
