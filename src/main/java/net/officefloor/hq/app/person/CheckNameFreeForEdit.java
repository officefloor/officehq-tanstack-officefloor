package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit to a name another person on the sheet already has, or a contact another person on the sheet has. */
public class CheckNameFreeForEdit {
    public void check(@Val PersonRequest request, @Val Person person, PersonStore store)
            throws DuplicateNameException, DuplicateContactException {
        if (store.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new DuplicateNameException(request.name());
        }
        if (request.contact() != null && store.contactTaken(person.getSheetId(), request.contact(), person.getId())) {
            throw new DuplicateContactException(request.contact());
        }
    }
}
