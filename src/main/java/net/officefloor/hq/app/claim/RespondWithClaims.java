package net.officefloor.hq.app.claim;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claims, newest first. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository,
            ObjectResponse<ClaimsResponse> response) {
        response.send(ClaimsResponse.from(repository.findBySheetIdOrderByIdDesc(sheetId.trim())));
    }
}
