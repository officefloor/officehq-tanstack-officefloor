package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** A person being edited may not take a contact another person on the sheet already has. */
public class CheckPersonRecontact {
    public void check(@Val Person person, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonContactException {
        if (values.contact() != null && people.contactTaken(person.sheetId(), values.contact(), person.id())) {
            throw new DuplicatePersonContactException(values.contact());
        }
    }
}
