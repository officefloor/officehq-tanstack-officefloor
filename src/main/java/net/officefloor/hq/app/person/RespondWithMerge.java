package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as the store holds them now. */
public class RespondWithMerge {
    public void respond(@Val Merge merge, PersonRepository people, NameTree tree, SheetRepository sheets,
            ObjectResponse<PersonResponse> response) {
        Person into = people.find(merge.into().sheetId(), merge.into().id());
        response.send(PersonResponse.from(into, tree.clashing(into, people.aliases(into.sheetId(), into.id())),
                people.origin(into.sheetId(), into.id()), people.tags(into.sheetId(), into.id()),
                people.statementLines(into.sheetId(), into.id()),
                people.flagOn(into.sheetId(), into.id(), sheets.today(into.sheetId())),
                people.approver(into.sheetId(), into.id())));
    }
}
