package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Puts the person on hold, or takes them off it when they are held. Read afresh and locked, so
 *  two toggles at once happen one after the other. Not an edit: the person's version is untouched,
 *  so an editor open on them can still save. Publishes the person as they are now. */
public class HoldPerson {
    public void hold(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            People people, Out<Person> personOut) throws PersonNotFoundException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        person.setHeld(!person.isHeld());
        people.saveHold(person);
        personOut.set(person);
    }
}
