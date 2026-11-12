package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimStore store,
            ObjectResponse<List<ClaimResponse>> response) {
        List<Claim> claims = store.list(sheetId.trim());
        // Only the dates the claims round by are read here, so no policy date is needed.
        Rounding rounding = new Rounding(null, claims);
        response.send(claims.stream().map(c -> ClaimResponse.from(c, rounding)).toList());
    }
}
