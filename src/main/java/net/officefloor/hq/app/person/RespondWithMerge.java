package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as the store holds them now. */
public class RespondWithMerge {
    public void respond(@Val Merge merge, PersonRepository people, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(people.find(merge.into().sheetId(), merge.into().id())));
    }
}
