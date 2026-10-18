package net.officefloor.hq.app.claim;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleClerkLimit {
    public void handle(@Parameter ClerkLimitException ex, ObjectResponse<ResponseEntity<ClaimRefusal>> response) {
        response.send(new ResponseEntity<>(new ClaimRefusal("limit", ex.getMessage()), HttpStatus.FORBIDDEN));
    }
}
