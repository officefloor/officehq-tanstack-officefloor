package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPeriodCreated {
    public void respond(@Val Period period, ObjectResponse<ResponseEntity<PeriodResponse>> response) {
        response.send(new ResponseEntity<>(PeriodResponse.from(period), HttpStatus.CREATED));
    }
}
