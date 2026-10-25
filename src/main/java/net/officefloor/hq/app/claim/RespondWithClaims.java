package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val SheetResponse sheet, ClaimRepository repository,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(ClaimResponse::from).toList());
    }
}
