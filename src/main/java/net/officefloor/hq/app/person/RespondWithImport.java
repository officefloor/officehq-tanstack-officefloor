package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithImport {
    public void respond(@Val ImportResponse result, ObjectResponse<ResponseEntity<ImportResponse>> response) {
        response.send(new ResponseEntity<>(result, HttpStatus.CREATED));
    }
}
