package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, ObjectResponse<ClaimResponse> response, ClaimRepository claims, ClaimLogRepository log,
            SheetRepository sheets) {
        response.send(ClaimResponse.from(claim, claims, log, sheets.today(claim.sheetId())));
    }
}
