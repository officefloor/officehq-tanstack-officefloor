package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit made from a version of the person other than the one stored now. */
public class CheckPersonVersion {
    public void check(@Val PersonRequest request, @Val Person person) throws StalePersonException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new StalePersonException(String.valueOf(person.getId()));
        }
    }
}
