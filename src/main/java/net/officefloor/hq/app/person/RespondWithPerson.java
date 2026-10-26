package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, Sheets sheets, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, sheets.today(person.sheetId())));
    }
}
