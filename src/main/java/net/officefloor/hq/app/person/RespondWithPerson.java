package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, PersonRepository people, NameTree tree, SheetRepository sheets,
            ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, tree.clashing(person, people.aliases(person.sheetId(), person.id())),
                people.origin(person.sheetId(), person.id()), people.tags(person.sheetId(), person.id()),
                people.statementLines(person.sheetId(), person.id()),
                people.flagOn(person.sheetId(), person.id(), sheets.today(person.sheetId()))));
    }
}
