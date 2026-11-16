package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claims, newest first. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository claims,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(claims.list(sheetId.trim()).stream().map((c) -> ClaimResponse.from(c, claims)).toList());
    }
}
