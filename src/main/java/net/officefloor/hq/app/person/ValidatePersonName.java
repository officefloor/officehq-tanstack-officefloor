package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuse renaming a person to a name, or giving them a contact, another person on the sheet already has. */
public class ValidatePersonName {
    public void validate(@Val PersonRequest request, @Val Person loaded, PersonRepository repository)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        if (repository.nameInUse(loaded.sheetId(), request.name(), loaded.id())) {
            throw new DuplicatePersonNameException(request.name());
        }
        if (request.contact() != null && !request.contact().isEmpty()
                && repository.contactInUse(loaded.sheetId(), request.contact(), loaded.id())) {
            throw new DuplicatePersonContactException(request.contact());
        }
    }
}
