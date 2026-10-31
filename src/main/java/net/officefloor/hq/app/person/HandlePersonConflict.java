package net.officefloor.hq.app.person;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandlePersonConflict {
    public void handle(@Parameter PersonConflictException ex,
            ObjectResponse<ResponseEntity<PersonResponse>> response) {
        response.send(new ResponseEntity<>(ex.getCurrent(), HttpStatus.CONFLICT));
    }
}
