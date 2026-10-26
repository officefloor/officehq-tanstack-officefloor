package net.officefloor.hq.app.claim;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HandleClaimRemindRefused {
    public void handle(@Parameter ClaimRemindRefusedException ex, ObjectResponse<ResponseEntity<String>> response) {
        response.send(new ResponseEntity<>(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
