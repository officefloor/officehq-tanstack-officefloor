package net.officefloor.hq.app.patron;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPatronCreated {
    public void respond(@Val Patron patron, ObjectResponse<ResponseEntity<PatronResponse>> response) {
        response.send(new ResponseEntity<>(PatronResponse.from(patron), HttpStatus.CREATED));
    }
}
