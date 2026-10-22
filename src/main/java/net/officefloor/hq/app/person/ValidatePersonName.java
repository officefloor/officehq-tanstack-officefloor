package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuse renaming a person to a name, or giving them a contact, another person on the sheet already has; and refuse
 *  any edit leaving their name, or one of their aliases, a name or alias on another sheet of the sheet's tree. */
public class ValidatePersonName {
    public void validate(@Val PersonRequest request, @Val Person loaded, PersonRepository repository)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        if (repository.nameInUse(loaded.sheetId(), request.name(), loaded.id())) {
            throw new DuplicatePersonNameException(request.name());
        }
        // A person who already clashes may be edited only by a rename to a name, and with aliases, that do not.
        DuplicatePersonNameException.refuseInTree(loaded.sheetId(), request.name(), repository);
        for (String alias : loaded.aliases()) {
            DuplicatePersonNameException.refuseInTree(loaded.sheetId(), alias, repository);
        }
        if (request.contact() != null && !request.contact().isEmpty()
                && repository.contactInUse(loaded.sheetId(), request.contact(), loaded.id())) {
            throw new DuplicatePersonContactException(request.contact());
        }
    }
}
