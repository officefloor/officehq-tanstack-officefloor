package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class SavePerson {
    public void save(@Val Person person, People people) throws PersonConflictException {
        // Saved over by someone else between this request's load and its save: refused as a conflict.
        if (!people.save(person)) {
            Person current = people.find(person.getSheetId(), person.getId());
            throw new PersonConflictException(PersonResponse.from(current != null ? current : person));
        }
    }
}
