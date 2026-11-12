package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Adds the alias to the person's aliases, refusing one that equals, ignoring case, any name or alias on the sheet. */
public class AddAlias {
    public void add(@Val AliasRequest request, @Val Person person, PersonStore store) throws DuplicateNameException {
        if (store.nameTaken(person.getSheetId(), request.alias(), null)) {
            throw new DuplicateNameException(request.alias());
        }
        store.addAlias(person, request.alias());
    }
}
