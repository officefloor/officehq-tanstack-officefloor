package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithVoidReasonCreated {
    public void respond(@Val VoidReasonValues values, ObjectResponse<ResponseEntity<VoidReason>> response) {
        response.send(new ResponseEntity<>(new VoidReason(values.reason(), 0), HttpStatus.CREATED));
    }
}
