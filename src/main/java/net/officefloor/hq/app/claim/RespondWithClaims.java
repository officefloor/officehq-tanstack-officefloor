package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val List<Claim> claims, ObjectResponse<List<ClaimResponse>> response) {
        response.send(claims.stream().map(ClaimResponse::from).toList());
    }
}
