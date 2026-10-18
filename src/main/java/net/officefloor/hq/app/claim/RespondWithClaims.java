package net.officefloor.hq.app.claim;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's claims, newest first, each with its status log. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository, ClaimLogRepository log,
            ObjectResponse<ClaimsResponse> response) {
        String sheet = sheetId.trim();
        response.send(ClaimsResponse.from(repository.findBySheetIdOrderByIdDesc(sheet), log.findBySheetIdOrderByIdAsc(sheet)));
    }
}
