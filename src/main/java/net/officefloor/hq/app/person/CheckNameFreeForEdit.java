package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit to a name another person on the sheet already has. */
public class CheckNameFreeForEdit {
    public void check(@Val PersonRequest request, @Val Person person, PersonStore store)
            throws DuplicateNameException {
        if (store.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new DuplicateNameException(request.name());
        }
    }
}
