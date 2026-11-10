package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses a person whose contact another person on the sheet already has. A person with no contact passes. */
public class CheckPersonContact {
    public void check(@Val Person person, PersonRepository repository)
            throws DuplicatePersonContactException {
        if (person.getContact() != null
                && repository.contactTaken(person.getSheetId(), person.getContact(), person.getId())) {
            throw new DuplicatePersonContactException(person.getContact());
        }
    }
}
