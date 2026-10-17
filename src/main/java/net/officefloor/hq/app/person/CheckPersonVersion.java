package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit made against a version of the person that someone else has since saved over. */
public class CheckPersonVersion {
    public void check(@Val PersonRequest request, @Val Person person) throws PersonConflictException {
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new PersonConflictException("This person was changed by someone else; their current values are shown.");
        }
    }
}
