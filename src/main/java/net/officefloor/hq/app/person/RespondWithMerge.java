package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as stored now. */
public class RespondWithMerge {
    public void respond(@Val PersonMerge merge, PersonRepository people, ObjectResponse<Person> response) {
        response.send(people.find(merge.into().sheetId(), merge.into().id()));
    }
}
