package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses a person whose name another person on the sheet already has. */
public class CheckPersonName {
    public void check(@Val Person person, PersonRepository repository)
            throws DuplicatePersonNameException {
        if (repository.nameTaken(person.getSheetId(), person.getName(), person.getId())) {
            throw new DuplicatePersonNameException(person.getName());
        }
    }
}
