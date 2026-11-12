package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as they stand now. */
public class RespondWithMerged {
    public void respond(@Val MergedInto merged, StatementStore statements, PatronStore patrons, SheetStore sheets,
            ObjectResponse<PersonResponse> response) {
        Person person = merged.person();
        response.send(PersonResponse.from(person, sheets.today(person.getSheetId()),
                statements.summary(person.getSheetId(), person.getId()),
                patrons.of(person.getSheetId(), person.getId())));
    }
}
