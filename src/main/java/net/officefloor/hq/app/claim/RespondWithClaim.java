package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, SheetRepository sheets, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim, java.util.List.of(), sheets.today(claim.getSheetId())));
    }
}
