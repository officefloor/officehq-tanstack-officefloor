package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person restored, as they stand now. */
public class RespondWithMergeUndone {
    public void respond(@Val MergeUndone undone, ObjectResponse<Person> response) {
        response.send(undone.restored());
    }
}
