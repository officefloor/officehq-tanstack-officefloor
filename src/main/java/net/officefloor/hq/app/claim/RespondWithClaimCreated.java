package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithClaimCreated {
    public void respond(@Val Claim claim, Figures figures, ClaimLog log, ObjectResponse<ResponseEntity<ClaimResponse>> response) {
        response.send(new ResponseEntity<>(ClaimResponse.from(claim, figures, log.of(claim), List.of()), HttpStatus.CREATED));
    }
}
