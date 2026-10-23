package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithOpeningImport {
    public void respond(@Val OpeningImportResponse result,
            ObjectResponse<ResponseEntity<OpeningImportResponse>> response) {
        response.send(new ResponseEntity<>(result, HttpStatus.CREATED));
    }
}
