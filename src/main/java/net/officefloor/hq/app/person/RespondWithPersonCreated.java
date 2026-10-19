package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPersonCreated {
    public void respond(@Val Person person, SheetClock clock, ObjectResponse<ResponseEntity<PersonResponse>> response) {
        response.send(new ResponseEntity<>(PersonResponse.from(person, clock.today(person.getSheetId())), HttpStatus.CREATED));
    }
}
