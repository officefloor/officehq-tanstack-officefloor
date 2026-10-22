package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPaste {
    public void respond(@Val PasteResponse result, ObjectResponse<ResponseEntity<PasteResponse>> response) {
        response.send(new ResponseEntity<>(result, HttpStatus.CREATED));
    }
}
