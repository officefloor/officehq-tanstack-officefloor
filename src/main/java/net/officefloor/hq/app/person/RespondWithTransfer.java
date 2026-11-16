package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person transferred, as stored now on the sheet they went to (the person merged into, when merged). */
public class RespondWithTransfer {
    public void respond(@Val PersonTransfer transfer, PersonRepository people, ObjectResponse<Person> response) {
        long id = transfer.into() != null ? transfer.into().id() : transfer.person().id();
        response.send(people.find(transfer.toSheet(), id));
    }
}
