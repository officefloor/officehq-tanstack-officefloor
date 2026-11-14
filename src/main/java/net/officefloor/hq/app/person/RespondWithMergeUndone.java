package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person restored by the undo, as the store holds them now. */
public class RespondWithMergeUndone {
    public void respond(@Val MergeMade made, @Val Person person, PersonRepository people, NameTree tree,
            SheetRepository sheets, ObjectResponse<PersonResponse> response) {
        Person back = people.find(person.sheetId(), made.personId());
        response.send(PersonResponse.from(back, tree.clashing(back, people.aliases(back.sheetId(), back.id())),
                people.origin(back.sheetId(), back.id()), people.tags(back.sheetId(), back.id()),
                people.statementLines(back.sheetId(), back.id()),
                people.flagOn(back.sheetId(), back.id(), sheets.today(back.sheetId())),
                people.approver(back.sheetId(), back.id())));
    }
}
