package net.officefloor.hq.app.holiday;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithHolidayCreated {
    public void respond(@Val HolidayValues values, ObjectResponse<ResponseEntity<Holiday>> response) {
        response.send(new ResponseEntity<>(new Holiday(values.date().toString()), HttpStatus.CREATED));
    }
}
