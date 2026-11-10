package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val Sheet sheet, ClaimRepository repository,
            ObjectResponse<ClaimsResponse> response) {
        response.send(new ClaimsResponse(
                repository.findAll(sheet.id()).stream().map(ClaimResponse::from).toList()));
    }
}
