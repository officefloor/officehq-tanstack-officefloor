package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithDelegationCreated {
    public void respond(@Val Delegation delegation, ObjectResponse<ResponseEntity<Delegation>> response) {
        response.send(new ResponseEntity<>(delegation, HttpStatus.CREATED));
    }
}
