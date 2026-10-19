package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses renaming a person to a name another person on the sheet already uses (ignoring case). */
public class CheckPersonRename {
    public void check(@Val PersonRequest request, @Val Person person, PersonRepository repository)
            throws InvalidPersonException {
        if (repository.nameInUse(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidPersonException("That name is already in use on this sheet.");
        }
    }
}
