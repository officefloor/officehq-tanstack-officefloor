package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Flags the person, replacing any flag they had. Not an edit: the person's version is untouched. */
public class FlagPerson {
    public void flag(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val FlagChange change, People people, Out<Person> personOut) throws PersonNotFoundException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        person.setFlag(change.reason(), change.until());
        people.saveFlag(person);
        personOut.set(person);
    }
}
