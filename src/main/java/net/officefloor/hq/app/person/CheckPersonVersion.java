package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit made from a form that loaded an older version of the person than the store holds. */
public class CheckPersonVersion {
    public void check(@Val PersonRequest request, @Val Person person) throws PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException();
        }
    }
}
