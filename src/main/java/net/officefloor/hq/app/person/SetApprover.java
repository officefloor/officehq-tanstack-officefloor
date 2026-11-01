package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the person's approver, replacing any they had. Not an edit: the person's version is untouched. */
public class SetApprover {
    public void set(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val ApproverChange change, People people, Out<Person> personOut) throws PersonNotFoundException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        person.setApprover(change.approver());
        people.saveApprover(person);
        personOut.set(person);
    }
}
