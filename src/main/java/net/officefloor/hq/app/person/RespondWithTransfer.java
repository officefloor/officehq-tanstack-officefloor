package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person as they arrived on the other sheet. */
public class RespondWithTransfer {
    public void respond(@Val Transferred transferred, PersonRepository people, NameTree tree,
            ObjectResponse<PersonResponse> response) {
        Person person = transferred.person();
        response.send(PersonResponse.from(person, tree.clashing(person, people.aliases(person.sheetId(), person.id())),
                people.origin(person.sheetId(), person.id()), people.tags(person.sheetId(), person.id())));
    }
}
