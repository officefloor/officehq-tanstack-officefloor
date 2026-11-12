package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, ClaimStore store, ClaimLogStore log, SheetStore sheets, ObjectResponse<ClaimResponse> response) {
        Claim corrected = claim.getCorrects() == null ? null
                : store.findByNumber(claim.getSheetId(), claim.getPersonId(), claim.getCorrects());
        response.send(ClaimResponse.from(claim, corrected == null ? claim.getDate() : corrected.getDate()).withLog(log.lines(claim))
                .on(sheets.today(claim.getSheetId())));
    }
}
