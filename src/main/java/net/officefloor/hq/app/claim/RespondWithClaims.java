package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every claim on the sheet, newest first. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId, ClaimRepository repository,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(repository.findBySheet(sheetId).stream().map(ClaimResponse::from).toList());
    }
}
