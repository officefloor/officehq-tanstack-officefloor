package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses an edit whose editor loaded a version of the person that is no longer the stored one. */
public class CheckPersonVersion {
    public void check(@Val PersonRequest request, @Val Person person) throws StalePersonException {
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new StalePersonException();
        }
    }
}
