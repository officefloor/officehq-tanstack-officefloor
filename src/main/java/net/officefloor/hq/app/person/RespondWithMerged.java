package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as they stand now. */
public class RespondWithMerged {
    public void respond(@Val Merge merge, ObjectResponse<Person> response) {
        response.send(merge.into());
    }
}
