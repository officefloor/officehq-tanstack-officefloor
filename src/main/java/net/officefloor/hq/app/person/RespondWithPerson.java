package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, SheetClock clock, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, clock.today(person.getSheetId())));
    }
}
