package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithRateImport {
    public void respond(@Val RateImportResponse result, ObjectResponse<ResponseEntity<RateImportResponse>> response) {
        response.send(new ResponseEntity<>(result, HttpStatus.CREATED));
    }
}
