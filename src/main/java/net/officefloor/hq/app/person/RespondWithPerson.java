package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, SheetRepository sheets, ObjectResponse<PersonResponse> response) {
        LocalDate today = sheets.findById(person.getSheetId()).map((s) -> s.getToday()).orElseGet(LocalDate::now);
        response.send(PersonResponse.from(person, today));
    }
}
