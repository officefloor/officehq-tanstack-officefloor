package net.officefloor.hq.app.period;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleCappedPeriod {
    public void handle(@Parameter CappedPeriodException ex, ObjectResponse<ResponseEntity<String>> response) {
        response.send(new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT));
    }
}
