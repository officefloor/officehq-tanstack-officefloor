package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses a person whose name another person on the sheet already has, ignoring case, or whose contact another has. */
public class CheckPersonNameUnique {
    public void check(@Val Person person, PersonRepository repository) throws InvalidPersonException {
        if (repository.nameTaken(person.sheetId(), person.name(), person.id())) {
            throw new InvalidPersonException("The name " + person.name() + " is already in use.");
        }
        if (person.contact() != null && repository.contactTaken(person.sheetId(), person.contact(), person.id())) {
            throw new InvalidPersonException("The contact " + person.contact() + " is already in use.");
        }
    }
}
