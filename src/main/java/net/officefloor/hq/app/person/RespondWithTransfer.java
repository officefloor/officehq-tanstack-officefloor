package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person transferred, as stored now on the sheet they went to. */
public class RespondWithTransfer {
    public void respond(@Val PersonTransfer transfer, PersonRepository people, ObjectResponse<Person> response) {
        response.send(people.find(transfer.toSheet(), transfer.person().id()));
    }
}
