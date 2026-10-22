package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuse renaming a person to a name another person on the sheet already has. */
public class ValidatePersonName {
    public void validate(@Val PersonRequest request, @Val Person loaded, PersonRepository repository)
            throws DuplicatePersonNameException {
        if (repository.nameInUse(loaded.sheetId(), request.name(), loaded.id())) {
            throw new DuplicatePersonNameException(request.name());
        }
    }
}
