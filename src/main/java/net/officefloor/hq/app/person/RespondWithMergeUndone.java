package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person restored, as stored now. */
public class RespondWithMergeUndone {
    public void respond(@Val PersonMergeUndo undo, PersonRepository people, ObjectResponse<Person> response) {
        response.send(people.find(undo.into().sheetId(), undo.merge().fromId()));
    }
}
