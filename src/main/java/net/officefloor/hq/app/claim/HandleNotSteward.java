package net.officefloor.hq.app.claim;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleNotSteward {
    public void handle(@Parameter NotStewardException ex, ObjectResponse<ResponseEntity<ClaimRefusal>> response) {
        response.send(new ResponseEntity<>(new ClaimRefusal("steward", ex.getMessage()), HttpStatus.FORBIDDEN));
    }
}
