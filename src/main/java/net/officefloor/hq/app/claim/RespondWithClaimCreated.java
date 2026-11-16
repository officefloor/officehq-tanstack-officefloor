package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithClaimCreated {
    public void respond(@Val Claim claim, ObjectResponse<ResponseEntity<ClaimResponse>> response) {
        response.send(new ResponseEntity<>(ClaimResponse.from(claim), HttpStatus.CREATED));
    }
}
